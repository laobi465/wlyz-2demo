package com.jicek.license.shop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.auth.interceptor.AuthContext;
import com.jicek.license.card.entity.CardType;
import com.jicek.license.card.mapper.CardTypeMapper;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.h5.auth.H5AuthContext;
import com.jicek.license.pay.entity.PayOrder;
import com.jicek.license.pay.mapper.PayOrderMapper;
import com.jicek.license.shop.dto.H5CreateOrderDTO;
import com.jicek.license.shop.dto.H5CreateOrderResultDTO;
import com.jicek.license.shop.dto.H5ShopViewDTO;
import com.jicek.license.shop.dto.ShopDetailDTO;
import com.jicek.license.shop.dto.ShopProductDTO;
import com.jicek.license.shop.dto.ShopProductSaveDTO;
import com.jicek.license.shop.dto.ShopSaveDTO;
import com.jicek.license.shop.entity.Shop;
import com.jicek.license.shop.entity.ShopProduct;
import com.jicek.license.shop.mapper.ShopMapper;
import com.jicek.license.shop.mapper.ShopProductMapper;
import com.jicek.license.software.entity.Software;
import com.jicek.license.software.mapper.SoftwareMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 内嵌卡网服务
 * 作者: 极策k  日期: 2026-07-22
 *
 * 职责：
 *  - 开发者后台 CRUD（tenantId 从 AuthContext 获取）
 *  - 店铺开/关、商品上下架（status 切换）
 *  - H5 公开访问店铺首页（无需 X-H5-Token）
 *  - H5 下单（需 X-H5-Token，写入 jicek_pay_order 待支付记录，payUrl 占位）
 *
 * 安全铁律：
 *  - 所有后台操作校验资源 tenantId == AuthContext.currentTenantId()
 *  - H5 下单 tenantId 来自 H5AuthContext，softwareId 不强制校验（店铺已绑定 softwareId）
 *  - 卡类必须归属当前租户才能上架到店铺
 */
@Slf4j
@Service
public class ShopService {

    private final ShopMapper shopMapper;
    private final ShopProductMapper shopProductMapper;
    private final SoftwareMapper softwareMapper;
    private final CardTypeMapper cardTypeMapper;
    private final PayOrderMapper payOrderMapper;

    public ShopService(ShopMapper shopMapper,
                       ShopProductMapper shopProductMapper,
                       SoftwareMapper softwareMapper,
                       CardTypeMapper cardTypeMapper,
                       PayOrderMapper payOrderMapper) {
        this.shopMapper = shopMapper;
        this.shopProductMapper = shopProductMapper;
        this.softwareMapper = softwareMapper;
        this.cardTypeMapper = cardTypeMapper;
        this.payOrderMapper = payOrderMapper;
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /* ============ 开发者后台 CRUD ============ */

    /**
     * 创建店铺
     * @param dto 店铺信息（tenantId 从 AuthContext 获取）
     * @return 店铺详情
     * @throws ServiceException 软件归属校验失败 / path 同租户重复
     */
    @Transactional(rollbackFor = Exception.class)
    public ShopDetailDTO create(ShopSaveDTO dto) {
        Long tenantId = requireCurrentTenantId();
        validateSoftwareOwnership(dto.getSoftwareId(), tenantId);
        validatePathUnique(tenantId, dto.getPath(), null);

        LocalDateTime now = LocalDateTime.now();
        Shop shop = new Shop();
        shop.setTenantId(tenantId);
        shop.setSoftwareId(dto.getSoftwareId());
        shop.setName(dto.getName());
        shop.setPath(dto.getPath());
        shop.setDescription(dto.getDescription());
        shop.setContact(dto.getContact());
        shop.setStatus(dto.getStatus() != null ? dto.getStatus() : JicekConstants.SHOP_STATUS_OPEN);
        shop.setCreateTime(now);
        shop.setUpdateTime(now);
        shopMapper.insert(shop);

        log.info("【店铺】创建成功 id={} tenantId={} softwareId={} path={}",
                shop.getId(), tenantId, dto.getSoftwareId(), dto.getPath());
        return toDetailDTO(shop);
    }

    /**
     * 更新店铺
     * @param dto 店铺信息（id 必填）
     * @throws ServiceException 店铺不存在 / 非本租户 / 软件归属失败 / path 重复
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(ShopSaveDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException(ResultCode.FAIL, "更新时 id 不能为空");
        }
        Long tenantId = requireCurrentTenantId();
        Shop existing = requireOwnedShop(dto.getId(), tenantId);

        validateSoftwareOwnership(dto.getSoftwareId(), tenantId);
        validatePathUnique(tenantId, dto.getPath(), dto.getId());

        existing.setSoftwareId(dto.getSoftwareId());
        existing.setName(dto.getName());
        existing.setPath(dto.getPath());
        existing.setDescription(dto.getDescription());
        existing.setContact(dto.getContact());
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }
        existing.setUpdateTime(LocalDateTime.now());
        shopMapper.updateById(existing);

        log.info("【店铺】更新成功 id={} tenantId={}", dto.getId(), tenantId);
    }

    /**
     * 删除店铺（级联删除商品）
     * @param id 店铺 ID
     * @throws ServiceException 店铺不存在 / 非本租户
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long tenantId = requireCurrentTenantId();
        requireOwnedShop(id, tenantId);

        // 级联删除商品
        shopProductMapper.delete(new LambdaQueryWrapper<ShopProduct>()
                .eq(ShopProduct::getShopId, id));
        shopMapper.deleteById(id);

        log.info("【店铺】删除成功 id={} tenantId={}（含级联商品）", id, tenantId);
    }

    /**
     * 分页查询店铺
     */
    public Page<ShopDetailDTO> page(long current, long size, Long softwareId, String name, Integer status) {
        Long tenantId = requireCurrentTenantId();
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<Shop>()
                .eq(Shop::getTenantId, tenantId)
                .eq(softwareId != null, Shop::getSoftwareId, softwareId)
                .like(name != null && !name.isBlank(), Shop::getName, name)
                .eq(status != null, Shop::getStatus, status)
                .orderByDesc(Shop::getCreateTime);
        Page<Shop> page = shopMapper.selectPage(new Page<>(current, size), wrapper);

        // 批量查询 softwareName，避免 N+1
        Map<Long, String> softwareNameMap = loadSoftwareNames(
                page.getRecords().stream().map(Shop::getSoftwareId).distinct().toList(), tenantId);

        Page<ShopDetailDTO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(s -> toDetailDTO(s, softwareNameMap.get(s.getSoftwareId())))
                .toList());
        return result;
    }

    /**
     * 获取店铺详情
     */
    public ShopDetailDTO get(Long id) {
        Long tenantId = requireCurrentTenantId();
        Shop shop = requireOwnedShop(id, tenantId);
        Software software = softwareMapper.selectById(shop.getSoftwareId());
        return toDetailDTO(shop, software != null ? software.getName() : null);
    }

    /**
     * 开启店铺
     */
    @Transactional(rollbackFor = Exception.class)
    public void openShop(Long id) {
        toggleShopStatus(id, JicekConstants.SHOP_STATUS_OPEN);
        log.info("【店铺】开启成功 id={}", id);
    }

    /**
     * 关闭店铺
     */
    @Transactional(rollbackFor = Exception.class)
    public void closeShop(Long id) {
        toggleShopStatus(id, JicekConstants.SHOP_STATUS_CLOSED);
        log.info("【店铺】关闭成功 id={}", id);
    }

    private void toggleShopStatus(Long id, int targetStatus) {
        Long tenantId = requireCurrentTenantId();
        Shop shop = requireOwnedShop(id, tenantId);
        shop.setStatus(targetStatus);
        shop.setUpdateTime(LocalDateTime.now());
        shopMapper.updateById(shop);
    }

    /* ============ 商品管理 ============ */

    /**
     * 添加商品
     * @param dto 商品信息
     * @return 商品详情
     * @throws ServiceException 店铺/卡类归属失败 / 卡类同店铺已上架
     */
    @Transactional(rollbackFor = Exception.class)
    public ShopProductDTO addProduct(ShopProductSaveDTO dto) {
        Long tenantId = requireCurrentTenantId();
        requireOwnedShop(dto.getShopId(), tenantId);
        validateCardTypeOwnership(dto.getCardTypeId(), tenantId);

        // 卡类同店铺不重复
        Long exists = shopProductMapper.selectCount(new LambdaQueryWrapper<ShopProduct>()
                .eq(ShopProduct::getShopId, dto.getShopId())
                .eq(ShopProduct::getCardTypeId, dto.getCardTypeId()));
        if (exists != null && exists > 0) {
            throw new ServiceException(ResultCode.FAIL, "该卡类已在本店铺上架，不能重复添加");
        }

        LocalDateTime now = LocalDateTime.now();
        ShopProduct product = new ShopProduct();
        product.setTenantId(tenantId);
        product.setShopId(dto.getShopId());
        product.setCardTypeId(dto.getCardTypeId());
        product.setPrice(dto.getPrice());
        product.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        product.setStatus(dto.getStatus() != null ? dto.getStatus() : JicekConstants.SHOP_PRODUCT_ON_SHELF);
        product.setCreateTime(now);
        product.setUpdateTime(now);
        shopProductMapper.insert(product);

        log.info("【店铺商品】添加成功 id={} shopId={} cardTypeId={} tenantId={}",
                product.getId(), dto.getShopId(), dto.getCardTypeId(), tenantId);
        CardType cardType = cardTypeMapper.selectById(dto.getCardTypeId());
        return toProductDTO(product, cardType != null ? cardType : new CardType());
    }

    /**
     * 更新商品
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(ShopProductSaveDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException(ResultCode.FAIL, "更新商品时 id 不能为空");
        }
        Long tenantId = requireCurrentTenantId();
        ShopProduct existing = requireOwnedProduct(dto.getId(), tenantId);
        // 校验店铺归属（防止跨店铺改商品）
        requireOwnedShop(dto.getShopId(), tenantId);
        validateCardTypeOwnership(dto.getCardTypeId(), tenantId);

        // 卡类同店铺不重复（排除自身）
        Long exists = shopProductMapper.selectCount(new LambdaQueryWrapper<ShopProduct>()
                .eq(ShopProduct::getShopId, dto.getShopId())
                .eq(ShopProduct::getCardTypeId, dto.getCardTypeId())
                .ne(ShopProduct::getId, dto.getId()));
        if (exists != null && exists > 0) {
            throw new ServiceException(ResultCode.FAIL, "该卡类已在本店铺上架，不能重复添加");
        }

        existing.setShopId(dto.getShopId());
        existing.setCardTypeId(dto.getCardTypeId());
        existing.setPrice(dto.getPrice());
        existing.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : existing.getSortOrder());
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }
        existing.setUpdateTime(LocalDateTime.now());
        shopProductMapper.updateById(existing);

        log.info("【店铺商品】更新成功 id={} tenantId={}", dto.getId(), tenantId);
    }

    /**
     * 删除商品
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeProduct(Long shopId, Long productId) {
        Long tenantId = requireCurrentTenantId();
        requireOwnedShop(shopId, tenantId);
        ShopProduct product = requireOwnedProduct(productId, tenantId);
        if (!product.getShopId().equals(shopId)) {
            throw new ServiceException(ResultCode.H5_PRODUCT_NOT_FOUND, "商品不属于该店铺");
        }
        shopProductMapper.deleteById(productId);
        log.info("【店铺商品】删除成功 id={} shopId={} tenantId={}", productId, shopId, tenantId);
    }

    /**
     * 列出店铺所有商品（含下架）
     */
    public List<ShopProductDTO> listProducts(Long shopId) {
        Long tenantId = requireCurrentTenantId();
        requireOwnedShop(shopId, tenantId);
        return listProductDTOs(shopId, null);
    }

    /* ============ H5 接口 ============ */

    /**
     * H5 公开访问店铺首页（无需 X-H5-Token）
     * @param path 店铺访问路径
     * @return 店铺视图（含上架商品列表）
     * @throws ServiceException 店铺不存在 / 已关闭
     */
    public H5ShopViewDTO getShopByPath(String path) {
        if (path == null || path.isBlank()) {
            throw new ServiceException(ResultCode.H5_SHOP_NOT_FOUND, "店铺路径不能为空");
        }
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getPath, path)
                .last("LIMIT 1"));
        if (shop == null) {
            throw new ServiceException(ResultCode.H5_SHOP_NOT_FOUND);
        }
        if (shop.getStatus() == null || shop.getStatus() != JicekConstants.SHOP_STATUS_OPEN) {
            throw new ServiceException(ResultCode.H5_SHOP_DISABLED);
        }

        Software software = softwareMapper.selectById(shop.getSoftwareId());
        H5ShopViewDTO view = new H5ShopViewDTO();
        view.setId(shop.getId());
        view.setName(shop.getName());
        view.setDescription(shop.getDescription());
        view.setContact(shop.getContact());
        view.setSoftwareName(software != null ? software.getName() : null);

        // 仅返回上架商品
        view.setProducts(listProductDTOs(shop.getId(), JicekConstants.SHOP_PRODUCT_ON_SHELF));
        return view;
    }

    /**
     * H5 下单（需 X-H5-Token）
     * <p>
     * 当前 v0.13.0 仅创建本地订单（status=0 待支付），payUrl 占位空字符串。
     * 后续接入彩虹易支付 V1 时填入实际支付链接。
     *
     * @param dto 下单请求
     * @return 下单结果（含订单号 + 金额 + 占位 payUrl）
     * @throws ServiceException 商品不存在 / 已下架 / 店铺已关闭
     */
    @Transactional(rollbackFor = Exception.class)
    public H5CreateOrderResultDTO createOrder(H5CreateOrderDTO dto) {
        Long tenantId = H5AuthContext.currentTenantId();
        if (tenantId == null) {
            throw new ServiceException(ResultCode.H5_TOKEN_INVALID, "H5 会话无效，无法下单");
        }

        // 二次校验数量（防绕过 Bean Validation 的边界场景）
        if (dto.getQuantity() == null || dto.getQuantity() < 1
                || dto.getQuantity() > JicekConstants.SHOP_ORDER_MAX_QUANTITY) {
            throw new ServiceException(ResultCode.FAIL,
                    "购买数量必须在 1-" + JicekConstants.SHOP_ORDER_MAX_QUANTITY + " 之间");
        }

        ShopProduct product = shopProductMapper.selectById(dto.getShopProductId());
        if (product == null || !product.getTenantId().equals(tenantId)) {
            throw new ServiceException(ResultCode.H5_PRODUCT_NOT_FOUND);
        }
        if (product.getStatus() == null
                || product.getStatus() != JicekConstants.SHOP_PRODUCT_ON_SHELF) {
            throw new ServiceException(ResultCode.H5_PRODUCT_NOT_FOUND, "商品已下架");
        }

        Shop shop = shopMapper.selectById(product.getShopId());
        if (shop == null || shop.getStatus() == null
                || shop.getStatus() != JicekConstants.SHOP_STATUS_OPEN) {
            throw new ServiceException(ResultCode.H5_SHOP_DISABLED, "店铺已关闭，无法下单");
        }

        CardType cardType = cardTypeMapper.selectById(product.getCardTypeId());
        if (cardType == null) {
            throw new ServiceException(ResultCode.CARD_TYPE_NOT_FOUND);
        }

        // 金额计算（禁 float，强制 BigDecimal）
        BigDecimal amount = product.getPrice()
                .multiply(BigDecimal.valueOf(dto.getQuantity()));

        // 写入 jicek_pay_order 待支付记录
        LocalDateTime now = LocalDateTime.now();
        PayOrder order = new PayOrder();
        order.setTenantId(tenantId);
        order.setOutTradeNo(generateOutTradeNo());
        order.setCardTypeId(product.getCardTypeId());
        order.setQuantity(dto.getQuantity());
        order.setAmount(amount);
        order.setPayType(dto.getPayType());
        order.setStatus(JicekConstants.ORDER_STATUS_PENDING);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        payOrderMapper.insert(order);

        log.info("【店铺订单】创建成功 outTradeNo={} tenantId={} shopProductId={} amount={} payType={}",
                order.getOutTradeNo(), tenantId, dto.getShopProductId(), amount, dto.getPayType());

        H5CreateOrderResultDTO result = new H5CreateOrderResultDTO();
        result.setOutTradeNo(order.getOutTradeNo());
        result.setAmount(amount);
        result.setPayType(dto.getPayType());
        result.setPayUrl("");  // 占位，后续接入支付网关时填入
        result.setShopName(shop.getName());
        result.setCardTypeName(cardType.getName());
        result.setQuantity(dto.getQuantity());
        return result;
    }

    /* ============ 内部工具 ============ */

    private Long requireCurrentTenantId() {
        Long tenantId = AuthContext.currentTenantId();
        if (tenantId == null) {
            throw new ServiceException(ResultCode.AUTH_NO_PERMISSION, "当前用户无租户身份");
        }
        return tenantId;
    }

    private void validateSoftwareOwnership(Long softwareId, Long tenantId) {
        Software software = softwareMapper.selectById(softwareId);
        if (software == null || !software.getTenantId().equals(tenantId)) {
            throw new ServiceException(ResultCode.SOFTWARE_PERMISSION_DENIED, "软件不存在或无权操作");
        }
    }

    private void validateCardTypeOwnership(Long cardTypeId, Long tenantId) {
        CardType cardType = cardTypeMapper.selectById(cardTypeId);
        if (cardType == null || !cardType.getTenantId().equals(tenantId)) {
            throw new ServiceException(ResultCode.CARD_TYPE_NOT_FOUND, "卡类不存在或无权操作");
        }
    }

    /**
     * 校验 path 在同租户下唯一（更新时排除自身）
     */
    private void validatePathUnique(Long tenantId, String path, Long excludeId) {
        Long count = shopMapper.selectCount(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getTenantId, tenantId)
                .eq(Shop::getPath, path)
                .ne(excludeId != null, Shop::getId, excludeId));
        if (count != null && count > 0) {
            throw new ServiceException(ResultCode.FAIL, "店铺路径在同租户下已存在");
        }
    }

    private Shop requireOwnedShop(Long id, Long tenantId) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null || !shop.getTenantId().equals(tenantId)) {
            throw new ServiceException(ResultCode.H5_SHOP_NOT_FOUND, "店铺不存在或无权操作");
        }
        return shop;
    }

    private ShopProduct requireOwnedProduct(Long id, Long tenantId) {
        ShopProduct product = shopProductMapper.selectById(id);
        if (product == null || !product.getTenantId().equals(tenantId)) {
            throw new ServiceException(ResultCode.H5_PRODUCT_NOT_FOUND, "商品不存在或无权操作");
        }
        return product;
    }

    /**
     * 批量加载软件名称（避免 N+1）
     */
    private Map<Long, String> loadSoftwareNames(List<Long> softwareIds, Long tenantId) {
        if (softwareIds.isEmpty()) {
            return Map.of();
        }
        return softwareMapper.selectList(new LambdaQueryWrapper<Software>()
                        .in(Software::getId, softwareIds)
                        .eq(Software::getTenantId, tenantId)).stream()
                .collect(Collectors.toMap(Software::getId, Software::getName, (a, b) -> a));
    }

    /**
     * 查询店铺商品列表并填充冗余字段（cardTypeName/cardType/type/duration/count/features/expireHint）
     * @param shopId 店铺 ID
     * @param statusFilter null=全部，1=仅上架，0=仅下架
     */
    private List<ShopProductDTO> listProductDTOs(Long shopId, Integer statusFilter) {
        List<ShopProduct> products = shopProductMapper.selectList(new LambdaQueryWrapper<ShopProduct>()
                .eq(ShopProduct::getShopId, shopId)
                .eq(statusFilter != null, ShopProduct::getStatus, statusFilter)
                .orderByDesc(ShopProduct::getSortOrder)
                .orderByDesc(ShopProduct::getId));
        if (products.isEmpty()) {
            return List.of();
        }

        // 批量加载卡类信息
        Map<Long, CardType> cardTypeMap = cardTypeMapper.selectList(new LambdaQueryWrapper<CardType>()
                .in(CardType::getId, products.stream().map(ShopProduct::getCardTypeId).distinct().toList()))
                .stream().collect(Collectors.toMap(CardType::getId, c -> c, (a, b) -> a));

        return products.stream().map(p -> {
            CardType cardType = cardTypeMap.get(p.getCardTypeId());
            return toProductDTO(p, cardType != null ? cardType : new CardType());
        }).toList();
    }

    private ShopDetailDTO toDetailDTO(Shop shop) {
        return toDetailDTO(shop, null);
    }

    private ShopDetailDTO toDetailDTO(Shop shop, String softwareName) {
        ShopDetailDTO dto = new ShopDetailDTO();
        dto.setId(shop.getId());
        dto.setTenantId(shop.getTenantId());
        dto.setSoftwareId(shop.getSoftwareId());
        dto.setName(shop.getName());
        dto.setPath(shop.getPath());
        dto.setDescription(shop.getDescription());
        dto.setContact(shop.getContact());
        dto.setStatus(shop.getStatus());
        dto.setCreateTime(shop.getCreateTime());
        dto.setUpdateTime(shop.getUpdateTime());
        dto.setSoftwareName(softwareName);
        return dto;
    }

    private ShopProductDTO toProductDTO(ShopProduct product, CardType cardType) {
        ShopProductDTO dto = new ShopProductDTO();
        dto.setId(product.getId());
        dto.setShopId(product.getShopId());
        dto.setCardTypeId(product.getCardTypeId());
        dto.setPrice(product.getPrice());
        dto.setSortOrder(product.getSortOrder());
        dto.setStatus(product.getStatus());
        dto.setCreateTime(product.getCreateTime());
        dto.setUpdateTime(product.getUpdateTime());
        // 冗余字段
        dto.setCardTypeName(cardType.getName());
        dto.setType(cardType.getType());
        dto.setDuration(cardType.getDuration());
        dto.setCount(cardType.getCount());
        dto.setFeatures(cardType.getFeatures());
        dto.setCardType(cardTypeCategoryName(cardType.getType()));
        dto.setExpireHint(buildExpireHint(cardType));
        return dto;
    }

    /**
     * 卡类中文名（按时长/次数/功能/永久）
     */
    private String cardTypeCategoryName(Integer type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case JicekConstants.CARD_TYPE_DURATION: return "时长卡";
            case JicekConstants.CARD_TYPE_COUNT: return "次数卡";
            case JicekConstants.CARD_TYPE_FEATURE: return "功能卡";
            case JicekConstants.CARD_TYPE_PERMANENT: return "永久卡";
            default: return "未知";
        }
    }

    /**
     * 构造到期说明（仅展示用，不参与业务计算）
     */
    private String buildExpireHint(CardType cardType) {
        if (cardType.getType() == null) {
            return null;
        }
        switch (cardType.getType()) {
            case JicekConstants.CARD_TYPE_DURATION:
                if (cardType.getDuration() == null) {
                    return "时长卡";
                }
                long days = cardType.getDuration() / 86400L;
                if (days > 0) {
                    return days + " 天";
                }
                long hours = cardType.getDuration() / 3600L;
                if (hours > 0) {
                    return hours + " 小时";
                }
                long minutes = cardType.getDuration() / 60L;
                return minutes + " 分钟";
            case JicekConstants.CARD_TYPE_COUNT:
                return cardType.getCount() != null ? cardType.getCount() + " 次" : "次数卡";
            case JicekConstants.CARD_TYPE_FEATURE:
                return cardType.getFeatures() != null ? "功能：" + cardType.getFeatures() : "功能卡";
            case JicekConstants.CARD_TYPE_PERMANENT:
                return "永久";
            default:
                return "未知";
        }
    }

    /**
     * 生成商户订单号（与 PayOrderService 同款格式：JC + yyyyMMddHHmmss + 6 位随机数）
     */
    private String generateOutTradeNo() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        return "JC" + timestamp + random;
    }
}
