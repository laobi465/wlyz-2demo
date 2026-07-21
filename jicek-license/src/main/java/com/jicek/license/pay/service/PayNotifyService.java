package com.jicek.license.pay.service;

import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.config.JicekProperties;
import com.jicek.license.pay.adapter.PayAdapter;
import com.jicek.license.pay.dto.PayNotifyDTO;
import com.jicek.license.pay.entity.PayConfig;
import com.jicek.license.pay.entity.PayOrder;
import com.jicek.license.transaction.PaymentTransactionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 异步回调处理服务
 * 作者: 极策k  日期: 2026-07-21
 *
 * 处理流程：
 * 1. 加分布式锁（按订单号，防并发回调）
 * 2. 验签（MD5）
 * 3. 幂等检查（订单状态非 0 拒绝重复处理）
 * 4. 金额一致性校验
 * 5. 调用资金事务服务（同事务：更新订单 + 发放卡密）
 *
 * 安全规范：
 * - 必须返回纯字符串 "success"（无 BOM、无空格、无 HTML）
 * - 重复回调仅记录日志，不重复发卡
 * - 任一环节失败返回非 success 字符串，让易支付重试
 */
@Slf4j
@Service
public class PayNotifyService {

    @Resource
    private PayConfigService payConfigService;

    @Resource
    private PayOrderStateMachineService orderStateMachineService;

    @Resource
    private PaymentTransactionService paymentTransactionService;

    @Resource
    private PayAdapter payAdapterV1;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private JicekProperties properties;

    /**
     * 处理异步回调
     * @param params 回调参数（GET 或 POST form）
     * @return "success" 表示处理成功，其他字符串表示失败
     */
    public String handleNotify(Map<String, String> params) {
        // 1. 解析回调数据
        PayNotifyDTO notify = payAdapterV1.parseNotify(params);
        if (notify.getOutTradeNo() == null) {
            log.warn("回调缺少 out_trade_no: {}", params);
            return "fail";
        }

        String lockKey = properties.getNotify().getLockPrefix() + notify.getOutTradeNo();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 2. 加分布式锁（防并发回调，等待 5s，持有 30s）
            boolean locked = lock.tryLock(5, properties.getNotify().getLockTimeout(), TimeUnit.SECONDS);
            if (!locked) {
                log.warn("回调锁获取失败，订单处理中: outTradeNo={}", notify.getOutTradeNo());
                return "fail";
            }

            // 3. 查询订单
            PayOrder order = orderStateMachineService.getByOutTradeNo(notify.getOutTradeNo());
            if (order == null) {
                log.warn("回调订单不存在: outTradeNo={}", notify.getOutTradeNo());
                return "fail";
            }

            // 4. 幂等检查：订单已非待支付状态，说明已处理过
            if (order.getStatus() != null && order.getStatus() != JicekConstants.ORDER_STATUS_PENDING) {
                log.info("重复回调，订单已处理: outTradeNo={}, status={}",
                        notify.getOutTradeNo(), order.getStatus());
                // 幂等返回 success，避免易支付反复重试
                return JicekConstants.EPAY_NOTIFY_RETURN_SUCCESS;
            }

            // 5. 获取支付配置（验签需要）
            PayConfig config = payConfigService.getByTenantId(order.getTenantId());

            // 6. 验签
            if (!payAdapterV1.verifyNotifySign(params, config)) {
                log.error("回调验签失败: outTradeNo={}", notify.getOutTradeNo());
                return "fail";
            }

            // 7. 校验交易状态（仅 TRADE_SUCCESS 才发卡）
            if (!"TRADE_SUCCESS".equalsIgnoreCase(notify.getTrade())) {
                log.warn("交易状态非成功: outTradeNo={}, trade={}",
                        notify.getOutTradeNo(), notify.getTrade());
                orderStateMachineService.markAsFailed(notify.getOutTradeNo(), "trade=" + notify.getTrade());
                return JicekConstants.EPAY_NOTIFY_RETURN_SUCCESS;
            }

            // 8. 调用资金事务服务（同事务：订单流转 + 卡密发放）
            // 此处为资金安全核心，详见 PaymentTransactionService
            paymentTransactionService.processPaymentSuccess(order, notify);

            return JicekConstants.EPAY_NOTIFY_RETURN_SUCCESS;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("回调处理被中断: outTradeNo={}", notify.getOutTradeNo(), e);
            return "fail";
        } catch (ServiceException e) {
            log.error("回调处理业务异常: outTradeNo={}, code={}, msg={}",
                    notify.getOutTradeNo(), e.getCode(), e.getMessage());
            return "fail";
        } catch (Exception e) {
            log.error("回调处理系统异常: outTradeNo={}", notify.getOutTradeNo(), e);
            return "fail";
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
