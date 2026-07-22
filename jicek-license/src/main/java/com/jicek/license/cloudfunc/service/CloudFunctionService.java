package com.jicek.license.cloudfunc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.cloudfunc.dto.CloudFunctionInvokeDTO;
import com.jicek.license.cloudfunc.dto.CloudFunctionInvokeResult;
import com.jicek.license.cloudfunc.dto.CloudFunctionSaveDTO;
import com.jicek.license.cloudfunc.entity.CloudFunction;
import com.jicek.license.cloudfunc.entity.CloudFunctionLog;
import com.jicek.license.cloudfunc.mapper.CloudFunctionLogMapper;
import com.jicek.license.cloudfunc.mapper.CloudFunctionMapper;
import com.jicek.license.cloudfunc.sandbox.LuaSandboxService;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 云函数业务服务
 * 作者: 极策k  日期: 2026-07-22
 *
 * 职责：
 * 1. 云函数 CRUD（同软件下 name 唯一）
 * 2. 执行调度：校验 → 注入输入 → 沙箱执行 → 落审计日志 → 更新统计
 * 3. 参数默认值填充（铁律 04：禁硬编码，所有默认走 JicekConstants 常量）
 * 4. 输入大小二次校验（防绕过 DTO 注解的攻击）
 *
 * 安全铁律：
 * - enabled=0 禁止调用（CF_DISABLED）
 * - timeoutMs/memoryLimitKb/maxInputKb/maxOutputKb 上限走常量（防恶意超大值）
 * - 每次执行必须落审计日志（成功/失败均落）
 * - 审计日志表禁 UPDATE/DELETE
 */
@Slf4j
@Service
public class CloudFunctionService {

    private final CloudFunctionMapper cfMapper;
    private final CloudFunctionLogMapper logMapper;
    private final LuaSandboxService luaSandbox;

    public CloudFunctionService(CloudFunctionMapper cfMapper,
                                 CloudFunctionLogMapper logMapper,
                                 LuaSandboxService luaSandbox) {
        this.cfMapper = cfMapper;
        this.logMapper = logMapper;
        this.luaSandbox = luaSandbox;
    }

    /* ============ CRUD ============ */

    /**
     * 新建或更新云函数
     * - id 为 null 时新建，否则更新（version 自增）
     * - 同软件下 name 必须唯一
     * - 代码长度 ≤ CF_CODE_MAX_BYTES
     * - 默认值填充：timeoutMs / memoryLimitKb / maxInputKb / maxOutputKb
     */
    @Transactional(rollbackFor = Exception.class)
    public Long save(CloudFunctionSaveDTO dto) {
        // 1. 代码长度校验
        int codeBytes = dto.getCode().getBytes(StandardCharsets.UTF_8).length;
        if (codeBytes > JicekConstants.CF_CODE_MAX_BYTES) {
            throw new ServiceException(ResultCode.CF_CODE_TOO_LARGE);
        }

        // 2. 默认值填充
        CloudFunction entity = new CloudFunction();
        if (dto.getId() != null) {
            CloudFunction existing = cfMapper.selectById(dto.getId());
            if (existing == null || !existing.getTenantId().equals(dto.getTenantId())) {
                throw new ServiceException(ResultCode.CF_NOT_FOUND);
            }
            entity = existing;
            entity.setVersion(existing.getVersion() + 1);
        } else {
            entity.setVersion(1);
            entity.setInvokeCount(0L);
            entity.setRuntime(JicekConstants.CF_RUNTIME_LUA);
            entity.setCreateTime(LocalDateTime.now());
        }

        entity.setTenantId(dto.getTenantId());
        entity.setSoftwareId(dto.getSoftwareId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setCode(dto.getCode());
        entity.setTimeoutMs(dto.getTimeoutMs() != null ? dto.getTimeoutMs() : JicekConstants.CF_DEFAULT_TIMEOUT_MS);
        entity.setMemoryLimitKb(dto.getMemoryLimitKb() != null ? dto.getMemoryLimitKb() : JicekConstants.CF_DEFAULT_MEMORY_KB);
        entity.setMaxInputKb(dto.getMaxInputKb() != null ? dto.getMaxInputKb() : JicekConstants.CF_DEFAULT_INPUT_KB);
        entity.setMaxOutputKb(dto.getMaxOutputKb() != null ? dto.getMaxOutputKb() : JicekConstants.CF_DEFAULT_OUTPUT_KB);
        entity.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : 1);
        entity.setUpdateTime(LocalDateTime.now());

        // 3. name 唯一性校验（同软件下）
        Long exists = cfMapper.selectCount(
                new LambdaQueryWrapper<CloudFunction>()
                        .eq(CloudFunction::getTenantId, dto.getTenantId())
                        .eq(CloudFunction::getSoftwareId, dto.getSoftwareId())
                        .eq(CloudFunction::getName, dto.getName())
                        .ne(dto.getId() != null, CloudFunction::getId, dto.getId()));
        if (exists != null && exists > 0) {
            throw new ServiceException(ResultCode.CF_NAME_EXISTS);
        }

        if (dto.getId() == null) {
            cfMapper.insert(entity);
        } else {
            cfMapper.updateById(entity);
        }
        return entity.getId();
    }

    /**
     * 分页查询云函数
     */
    public Page<CloudFunction> page(Long tenantId, Long softwareId, String name, Integer enabled,
                                     int current, int size) {
        return cfMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<CloudFunction>()
                        .eq(CloudFunction::getTenantId, tenantId)
                        .eq(softwareId != null, CloudFunction::getSoftwareId, softwareId)
                        .like(name != null && !name.isEmpty(), CloudFunction::getName, name)
                        .eq(enabled != null, CloudFunction::getEnabled, enabled)
                        .orderByDesc(CloudFunction::getCreateTime));
    }

    /**
     * 详情查询
     */
    public CloudFunction get(Long tenantId, Long functionId) {
        CloudFunction cf = cfMapper.selectById(functionId);
        if (cf == null || !cf.getTenantId().equals(tenantId)) {
            throw new ServiceException(ResultCode.CF_NOT_FOUND);
        }
        return cf;
    }

    /**
     * 按软件 + 函数名查询云函数（SDK 端按 name 调用时用）
     * 不存在返回 null（由调用方决定如何处理，便于区分 CF_NOT_FOUND / CF_DISABLED）
     */
    public CloudFunction findBySoftwareAndName(Long tenantId, Long softwareId, String name) {
        return cfMapper.selectOne(
                new LambdaQueryWrapper<CloudFunction>()
                        .eq(CloudFunction::getTenantId, tenantId)
                        .eq(CloudFunction::getSoftwareId, softwareId)
                        .eq(CloudFunction::getName, name));
    }

    /**
     * 删除（带租户隔离校验）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long tenantId, Long functionId) {
        CloudFunction cf = get(tenantId, functionId);
        cfMapper.deleteById(cf.getId());
    }

    /**
     * 启用/禁用切换
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleEnabled(Long tenantId, Long functionId, int enabled) {
        if (enabled != 0 && enabled != 1) {
            throw new ServiceException(ResultCode.CF_PARAM_INVALID, "enabled 必须为 0 或 1");
        }
        CloudFunction cf = get(tenantId, functionId);
        cf.setEnabled(enabled);
        cf.setUpdateTime(LocalDateTime.now());
        cfMapper.updateById(cf);
    }

    /* ============ 执行 ============ */

    /**
     * 调用云函数
     *
     * @param dto          调用请求
     * @param invokeSource 调用来源：dev / sdk
     * @param callerIp     调用方 IP（用于审计）
     */
    public CloudFunctionInvokeResult invoke(CloudFunctionInvokeDTO dto, String invokeSource, String callerIp) {
        CloudFunction cf = get(dto.getTenantId(), dto.getFunctionId());

        // 软件归属校验
        if (!cf.getSoftwareId().equals(dto.getSoftwareId())) {
            throw new ServiceException(ResultCode.CF_NOT_FOUND);
        }

        // 启用状态校验
        if (cf.getEnabled() == null || cf.getEnabled() != 1) {
            throw new ServiceException(ResultCode.CF_DISABLED);
        }

        // 输入大小校验（二次校验，防绕过 DTO 注解）
        String input = dto.getInput() == null ? "" : dto.getInput();
        int inputBytes = input.getBytes(StandardCharsets.UTF_8).length;
        int maxInputBytes = cf.getMaxInputKb() * 1024;
        if (inputBytes > maxInputBytes) {
            log.warn("云函数输入超限: func={}, inputBytes={}, maxBytes={}",
                    cf.getName(), inputBytes, maxInputBytes);
            return recordFailureLog(cf, invokeSource, callerIp, inputBytes, 0, 0,
                    JicekConstants.CF_STATUS_INPUT_LIMIT,
                    "输入 " + inputBytes + "B 超过上限 " + maxInputBytes + "B");
        }

        long startTime = System.currentTimeMillis();
        CloudFunctionInvokeResult result;
        try {
            String output = luaSandbox.execute(
                    cf.getCode(),
                    input,
                    cf.getTimeoutMs(),
                    cf.getMaxOutputKb());
            int durationMs = (int) (System.currentTimeMillis() - startTime);
            int outputBytes = output.getBytes(StandardCharsets.UTF_8).length;
            result = CloudFunctionInvokeResult.success(output, durationMs, inputBytes, outputBytes);
            recordSuccessLog(cf, invokeSource, callerIp, inputBytes, outputBytes, durationMs);
        } catch (ServiceException e) {
            int durationMs = (int) (System.currentTimeMillis() - startTime);
            int status = mapExceptionToStatus(e.getCode());
            result = CloudFunctionInvokeResult.fail(status, e.getMessage(), durationMs, inputBytes, 0);
            recordFailureLog(cf, invokeSource, callerIp, inputBytes, 0, durationMs, status, e.getMessage());
        } catch (Exception e) {
            int durationMs = (int) (System.currentTimeMillis() - startTime);
            log.error("云函数执行未知异常: func={}", cf.getName(), e);
            result = CloudFunctionInvokeResult.fail(
                    JicekConstants.CF_STATUS_RUNTIME_ERROR,
                    truncateError(e.getMessage()),
                    durationMs, inputBytes, 0);
            recordFailureLog(cf, invokeSource, callerIp, inputBytes, 0, durationMs,
                    JicekConstants.CF_STATUS_RUNTIME_ERROR, truncateError(e.getMessage()));
        }

        // 异步更新统计（不阻塞响应；@Transactional 不能跨越 try/catch，故用单独方法）
        updateInvokeStats(cf.getId(), callerIp);
        return result;
    }

    /* ============ 审计日志 ============ */

    /**
     * 审计日志分页查询
     */
    public Page<CloudFunctionLog> logPage(Long tenantId, Long functionId, Long softwareId,
                                           Integer status, String invokeSource,
                                           int current, int size) {
        return logMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<CloudFunctionLog>()
                        .eq(CloudFunctionLog::getTenantId, tenantId)
                        .eq(functionId != null, CloudFunctionLog::getFunctionId, functionId)
                        .eq(softwareId != null, CloudFunctionLog::getSoftwareId, softwareId)
                        .eq(status != null, CloudFunctionLog::getStatus, status)
                        .eq(invokeSource != null && !invokeSource.isEmpty(),
                                CloudFunctionLog::getInvokeSource, invokeSource)
                        .orderByDesc(CloudFunctionLog::getCreateTime));
    }

    /* ============ 私有方法 ============ */

    /**
     * 错误码 → 审计状态映射
     */
    private int mapExceptionToStatus(Integer code) {
        if (code == null) return JicekConstants.CF_STATUS_RUNTIME_ERROR;
        if (code.equals(ResultCode.CF_COMPILE_FAIL.getCode())) return JicekConstants.CF_STATUS_COMPILE_FAIL;
        if (code.equals(ResultCode.CF_TIMEOUT.getCode())) return JicekConstants.CF_STATUS_TIMEOUT;
        if (code.equals(ResultCode.CF_MEMORY_LIMIT.getCode())) return JicekConstants.CF_STATUS_MEMORY_LIMIT;
        if (code.equals(ResultCode.CF_INPUT_TOO_LARGE.getCode())) return JicekConstants.CF_STATUS_INPUT_LIMIT;
        if (code.equals(ResultCode.CF_OUTPUT_TOO_LARGE.getCode())) return JicekConstants.CF_STATUS_OUTPUT_LIMIT;
        return JicekConstants.CF_STATUS_RUNTIME_ERROR;
    }

    private void recordSuccessLog(CloudFunction cf, String source, String ip,
                                   int inputBytes, int outputBytes, int durationMs) {
        CloudFunctionLog log = new CloudFunctionLog();
        log.setTenantId(cf.getTenantId());
        log.setFunctionId(cf.getId());
        log.setFunctionName(cf.getName());
        log.setSoftwareId(cf.getSoftwareId());
        log.setInvokeSource(source);
        log.setCallerIp(ip);
        log.setInputSize(inputBytes);
        log.setOutputSize(outputBytes);
        log.setDurationMs(durationMs);
        log.setStatus(JicekConstants.CF_STATUS_SUCCESS);
        log.setErrorMessage(null);
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);
    }

    private CloudFunctionInvokeResult recordFailureLog(CloudFunction cf, String source, String ip,
                                                         int inputBytes, int outputBytes, int durationMs,
                                                         int status, String errorMsg) {
        CloudFunctionLog log = new CloudFunctionLog();
        log.setTenantId(cf.getTenantId());
        log.setFunctionId(cf.getId());
        log.setFunctionName(cf.getName());
        log.setSoftwareId(cf.getSoftwareId());
        log.setInvokeSource(source);
        log.setCallerIp(ip);
        log.setInputSize(inputBytes);
        log.setOutputSize(outputBytes);
        log.setDurationMs(durationMs);
        log.setStatus(status);
        log.setErrorMessage(truncateError(errorMsg));
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);
        return CloudFunctionInvokeResult.fail(status, truncateError(errorMsg), durationMs, inputBytes, outputBytes);
    }

    /**
     * 更新调用统计（独立事务，失败不影响主流程）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateInvokeStats(Long functionId, String callerIp) {
        try {
            CloudFunction cf = cfMapper.selectById(functionId);
            if (cf != null) {
                cf.setInvokeCount((cf.getInvokeCount() == null ? 0 : cf.getInvokeCount()) + 1);
                cf.setLastInvokeTime(LocalDateTime.now());
                cf.setLastInvokeIp(callerIp);
                cfMapper.updateById(cf);
            }
        } catch (Exception e) {
            log.warn("更新云函数调用统计失败: funcId={}", functionId, e);
        }
    }

    /**
     * 截断错误信息至 CF_ERROR_MSG_MAX_BYTES
     */
    private String truncateError(String msg) {
        if (msg == null) return "未知错误";
        if (msg.length() <= JicekConstants.CF_ERROR_MSG_MAX_BYTES) return msg;
        return msg.substring(0, JicekConstants.CF_ERROR_MSG_MAX_BYTES) + "...[truncated]";
    }
}
