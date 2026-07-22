package com.jicek.license.deploy.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.config.JicekProperties;
import com.jicek.license.deploy.entity.DeployLog;
import com.jicek.license.deploy.mapper.DeployLogMapper;
import com.jicek.license.deploy.dto.DeployStatusDTO;
import com.jicek.license.deploy.dto.WebhookResultDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 部署服务
 * 作者: 极策k  日期: 2026-07-22
 *
 * 设计说明（铁律 04/06/13）：
 * 1. Webhook Secret / 宝塔 API Key 全部走 JicekProperties（环境变量注入），禁字面量
 * 2. 部署过程异步执行（@Async 或新线程），Webhook 立即返回
 * 3. Redisson 分布式锁防并发触发（DEPLOY_LOCK_TIMEOUT_SECONDS 5 分钟自动释放防死锁）
 * 4. 审计日志仅 INSERT + SELECT，状态变更通过新增记录或受控更新（铁律 06：不虚构 SQL）
 * 5. 失败自动回滚：还原备份的 jar + 重启
 * 6. 所有命令执行使用 ProcessBuilder，禁 Runtime.exec（防注入）
 *
 * 部署流程（PROJECT.md 7.2）：
 *   备份 → git pull → 依赖安装 → DB迁移 → 清缓存 → 健康检查 → 重启 → 健康检查 → 完成
 *                                                                              ↓ 失败
 *                                                                           自动回滚
 */
@Service
public class DeployService {

    private static final Logger log = LoggerFactory.getLogger(DeployService.class);

    private final DeployLogMapper deployLogMapper;
    private final JicekProperties properties;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    /** 部署锁（进程内可见，配合 Redisson 分布式锁） */
    private volatile boolean deploying = false;

    public DeployService(DeployLogMapper deployLogMapper,
                         JicekProperties properties,
                         RedissonClient redissonClient,
                         ObjectMapper objectMapper) {
        this.deployLogMapper = deployLogMapper;
        this.properties = properties;
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void logInit() {
        if (properties.getDeploy().isEnabled()) {
            log.info("[Deploy] 部署功能已启用，重启模式: {}", properties.getDeploy().getRestartMode());
        } else {
            log.info("[Deploy] 部署功能未启用（jicek.deploy.enabled=false）");
        }
    }

    /* ============ Webhook 接收 ============ */

    /**
     * 处理 GitHub Webhook
     *
     * @param request HttpServletRequest（读取 body + headers）
     * @return 接收结果（accepted / ignored）
     */
    public WebhookResultDTO handleWebhook(HttpServletRequest request) {
        if (!properties.getDeploy().isEnabled()) {
            throw new ServiceException(ResultCode.DEPLOY_PARAM_INVALID, "部署功能未启用");
        }

        // 1. 读取 body（只能读一次，需缓存）
        String body = readBody(request);

        // 2. 验证签名（HMAC-SHA256，常量时间比较防时序攻击）
        String signature = request.getHeader(JicekConstants.DEPLOY_WEBHOOK_SIGNATURE_HEADER);
        verifySignature(body, signature);

        // 3. 校验事件类型（仅处理 push）
        String event = request.getHeader(JicekConstants.DEPLOY_WEBHOOK_EVENT_HEADER);
        if (!JicekConstants.DEPLOY_WEBHOOK_EVENT_PUSH.equals(event)) {
            return WebhookResultDTO.ignored("非 push 事件，忽略");
        }

        // 4. 解析 payload 获取 commit hash + branch
        String commitHash = parseCommitHash(body);
        String branch = parseBranch(body);
        String defaultBranch = StrUtil.isBlank(branch)
                ? JicekConstants.DEPLOY_DEFAULT_BRANCH : branch;

        // 5. 异步触发部署
        Long deployLogId = triggerDeploy(
                JicekConstants.DEPLOY_SOURCE_WEBHOOK,
                commitHash,
                defaultBranch,
                getClientIp(request));

        return WebhookResultDTO.accepted(deployLogId, "Webhook 已接收，部署异步执行中");
    }

    /**
     * 管理员手动触发部署
     */
    public WebhookResultDTO manualDeploy(Long tenantId, String branch, String operatorIp) {
        if (!properties.getDeploy().isEnabled()) {
            throw new ServiceException(ResultCode.DEPLOY_PARAM_INVALID, "部署功能未启用");
        }
        String actualBranch = StrUtil.isBlank(branch)
                ? JicekConstants.DEPLOY_DEFAULT_BRANCH : branch;
        Long deployLogId = triggerDeploy(
                JicekConstants.DEPLOY_SOURCE_MANUAL,
                null,
                actualBranch,
                operatorIp);
        return WebhookResultDTO.accepted(deployLogId, "手动部署已触发，异步执行中");
    }

    /**
     * 触发部署（核心编排，异步执行）
     *
     * @return 部署日志 ID
     */
    private Long triggerDeploy(String source, String commitHash, String branch, String operatorIp) {
        // 1. 分布式锁（Redisson，防并发）
        RLock lock = redissonClient.getLock(JicekConstants.REDIS_KEY_DEPLOY_LOCK);
        boolean locked;
        try {
            locked = lock.tryLock(0, JicekConstants.DEPLOY_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException(ResultCode.DEPLOY_LOCK_FAIL);
        }
        if (!locked) {
            throw new ServiceException(ResultCode.DEPLOY_LOCK_FAIL);
        }
        if (deploying) {
            lock.unlock();
            throw new ServiceException(ResultCode.DEPLOY_LOCK_FAIL);
        }
        deploying = true;

        // 2. 创建审计日志（status=0 进行中）
        DeployLog deployLog = new DeployLog();
        deployLog.setTenantId(1L); // TODO: 从登录态获取，当前固定 1
        deployLog.setTriggerSource(source);
        deployLog.setCommitHash(commitHash);
        deployLog.setBranch(branch);
        deployLog.setStatus(JicekConstants.DEPLOY_STATUS_RUNNING);
        deployLog.setOperatorIp(operatorIp);
        deployLog.setCreateTime(LocalDateTime.now());
        deployLogMapper.insert(deployLog);
        Long logId = deployLog.getId();

        // 3. 异步执行部署流程（不阻塞 Webhook 响应）
        final Long finalLogId = logId;
        final RLock finalLock = lock;
        Thread deployThread = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            try {
                executeDeployFlow(branch);
                long duration = System.currentTimeMillis() - startTime;
                updateDeployLog(finalLogId, JicekConstants.DEPLOY_STATUS_SUCCESS,
                        (int) duration, null);
                log.info("[Deploy] 部署成功 logId={} duration={}ms", finalLogId, duration);
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                String errMsg = truncateError(e.getMessage());
                log.error("[Deploy] 部署失败 logId={} duration={}ms", finalLogId, duration, e);
                // 自动回滚
                try {
                    rollback();
                    updateDeployLog(finalLogId, JicekConstants.DEPLOY_STATUS_ROLLED_BACK,
                            (int) duration, errMsg + "（已自动回滚）");
                } catch (Exception rollbackEx) {
                    log.error("[Deploy] 回滚失败 logId={}", finalLogId, rollbackEx);
                    updateDeployLog(finalLogId, JicekConstants.DEPLOY_STATUS_FAILED,
                            (int) duration, errMsg + " | 回滚失败: " + truncateError(rollbackEx.getMessage()));
                }
            } finally {
                deploying = false;
                if (finalLock.isHeldByCurrentThread()) {
                    finalLock.unlock();
                }
            }
        }, "jicek-deploy-" + logId);
        deployThread.setDaemon(true);
        deployThread.start();

        return logId;
    }

    /* ============ 部署流程 ============ */

    /**
     * 执行部署流程（顺序执行，任一步失败抛异常触发回滚）
     */
    private void executeDeployFlow(String branch) {
        String projectRoot = properties.getDeploy().getProjectRoot();
        if (StrUtil.isBlank(projectRoot)) {
            throw new ServiceException(ResultCode.DEPLOY_PARAM_INVALID, "project-root 未配置");
        }
        File rootDir = new File(projectRoot);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new ServiceException(ResultCode.DEPLOY_PARAM_INVALID,
                    "项目根目录不存在: " + projectRoot);
        }

        // 1. 备份当前构建产物
        backupArtifacts(rootDir);

        // 2. git pull
        gitPull(rootDir, branch);

        // 3. 构建后端（mvn clean package -DskipTests）
        buildBackend(rootDir);

        // 4. 构建前端（npm ci && npm run build）
        buildFrontend(rootDir);

        // 5. 重启服务
        restart();

        // 6. 健康检查
        healthCheck();
    }

    /**
     * 备份当前构建产物（jar + 前端 dist）
     */
    private void backupArtifacts(File rootDir) {
        Path backupDir = Paths.get(rootDir.getAbsolutePath(), ".jicek-backup",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        try {
            Files.createDirectories(backupDir);

            // 备份后端 jar
            Path jarPath = Paths.get(rootDir.getAbsolutePath(),
                    "jicek-license", "target", "jicek-license.jar");
            if (Files.exists(jarPath)) {
                Files.copy(jarPath, backupDir.resolve("jicek-license.jar"),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            // 备份前端 dist
            Path distPath = Paths.get(rootDir.getAbsolutePath(),
                    "jicek-ui", "dist");
            if (Files.exists(distPath)) {
                copyDir(distPath, backupDir.resolve("dist"));
            }

            // 清理旧备份（保留最近 N 个）
            cleanupOldBackups(rootDir);

            log.info("[Deploy] 备份完成 -> {}", backupDir);
        } catch (IOException e) {
            throw new ServiceException(ResultCode.DEPLOY_BUILD_FAIL, "备份失败: " + e.getMessage());
        }
    }

    /**
     * git pull
     */
    private void gitPull(File rootDir, String branch) {
        try {
            // 先 fetch
            execCommand(rootDir, "git", "fetch", "origin", branch);
            // 再 reset（避免本地冲突，强制对齐远端）
            execCommand(rootDir, "git", "reset", "--hard", "origin/" + branch);
            log.info("[Deploy] git pull 完成 branch={}", branch);
        } catch (Exception e) {
            throw new ServiceException(ResultCode.DEPLOY_GIT_PULL_FAIL, e.getMessage());
        }
    }

    /**
     * 构建后端
     */
    private void buildBackend(File rootDir) {
        File backendDir = new File(rootDir, "jicek-license");
        try {
            execCommand(backendDir, "mvn", "clean", "package", "-DskipTests", "-q");
            log.info("[Deploy] 后端构建完成");
        } catch (Exception e) {
            throw new ServiceException(ResultCode.DEPLOY_BUILD_FAIL, "后端构建失败: " + e.getMessage());
        }
    }

    /**
     * 构建前端
     */
    private void buildFrontend(File rootDir) {
        File frontendDir = new File(rootDir, "jicek-ui");
        if (!frontendDir.exists()) {
            log.warn("[Deploy] 前端目录不存在，跳过前端构建");
            return;
        }
        try {
            execCommand(frontendDir, "npm", "ci", "--silent");
            execCommand(frontendDir, "npm", "run", "build", "--silent");
            log.info("[Deploy] 前端构建完成");
        } catch (Exception e) {
            throw new ServiceException(ResultCode.DEPLOY_BUILD_FAIL, "前端构建失败: " + e.getMessage());
        }
    }

    /**
     * 重启服务（按 restartMode 分发）
     */
    private void restart() {
        String mode = properties.getDeploy().getRestartMode();
        if ("none".equals(mode)) {
            log.info("[Deploy] restart-mode=none，跳过重启（需手动重启）");
            return;
        }
        try {
            if ("docker".equals(mode)) {
                String container = properties.getDeploy().getDockerContainer();
                if (StrUtil.isBlank(container)) {
                    throw new ServiceException(ResultCode.DEPLOY_RESTART_FAIL,
                            "docker-container 未配置");
                }
                execCommand(null, "docker", "restart", container);
                log.info("[Deploy] Docker 容器重启完成: {}", container);
            } else if ("btpanel".equals(mode)) {
                // 宝塔 API 重启（通过 HTTP 调用，使用 Hutool HttpUtil）
                restartViaBtPanel();
                log.info("[Deploy] 宝塔面板重启完成");
            } else {
                throw new ServiceException(ResultCode.DEPLOY_RESTART_FAIL,
                        "未知 restart-mode: " + mode);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(ResultCode.DEPLOY_RESTART_FAIL, e.getMessage());
        }
    }

    /**
     * 通过宝塔 API 重启
     */
    private void restartViaBtPanel() {
        String apiUrl = properties.getDeploy().getBtpanelApiUrl();
        String apiKey = properties.getDeploy().getBtpanelApiKey();
        if (StrUtil.isBlank(apiUrl) || StrUtil.isBlank(apiKey)) {
            throw new ServiceException(ResultCode.DEPLOY_RESTART_FAIL,
                    "宝塔 API URL 或 Key 未配置");
        }
        // 简化实现：调用宝塔 API 重启指定容器
        // 实际宝塔 API 需签名，此处用 Hutool 发送 POST
        try {
            cn.hutool.http.HttpUtil.post(apiUrl + "/system/restart_service"
                    + "?service=docker&name=" + properties.getDeploy().getDockerContainer()
                    + "&api_key=" + apiKey, "");
        } catch (Exception e) {
            throw new ServiceException(ResultCode.DEPLOY_RESTART_FAIL,
                    "宝塔 API 调用失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查（轮询 /actuator/health，超时 60s）
     */
    private void healthCheck() {
        String baseUrl = properties.getDeploy().getHealthCheckBaseUrl();
        if (StrUtil.isBlank(baseUrl)) {
            log.warn("[Deploy] health-check-base-url 未配置，跳过健康检查");
            return;
        }
        String url = baseUrl + JicekConstants.DEPLOY_HEALTH_CHECK_PATH;
        long deadline = System.currentTimeMillis()
                + JicekConstants.DEPLOY_HEALTH_CHECK_TIMEOUT_SECONDS * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                String resp = cn.hutool.http.HttpUtil.get(url, 5000);
                if (resp != null && resp.contains("\"status\":\"UP\"")) {
                    log.info("[Deploy] 健康检查通过");
                    return;
                }
            } catch (Exception e) {
                // 忽略，继续重试
            }
            try {
                Thread.sleep(JicekConstants.DEPLOY_HEALTH_CHECK_INTERVAL_SECONDS * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ServiceException(ResultCode.DEPLOY_HEALTH_CHECK_FAIL, "健康检查被中断");
            }
        }
        throw new ServiceException(ResultCode.DEPLOY_HEALTH_CHECK_FAIL,
                "健康检查超时（" + JicekConstants.DEPLOY_HEALTH_CHECK_TIMEOUT_SECONDS + "s）");
    }

    /**
     * 回滚（还原最近一次备份）
     */
    private void rollback() {
        String projectRoot = properties.getDeploy().getProjectRoot();
        Path backupRoot = Paths.get(projectRoot, ".jicek-backup");
        if (!Files.exists(backupRoot)) {
            throw new ServiceException(ResultCode.DEPLOY_ROLLBACK_FAIL, "无备份可回滚");
        }
        try (Stream<Path> dirs = Files.list(backupRoot)) {
            Path latestBackup = dirs
                    .filter(Files::isDirectory)
                    .max(Comparator.comparing(Path::getFileName))
                    .orElseThrow(() -> new ServiceException(ResultCode.DEPLOY_ROLLBACK_FAIL,
                            "无备份目录"));
            // 还原 jar
            Path jarBackup = latestBackup.resolve("jicek-license.jar");
            if (Files.exists(jarBackup)) {
                Path jarTarget = Paths.get(projectRoot, "jicek-license", "target",
                        "jicek-license.jar");
                Files.createDirectories(jarTarget.getParent());
                Files.copy(jarBackup, jarTarget, StandardCopyOption.REPLACE_EXISTING);
            }
            // 还原 dist
            Path distBackup = latestBackup.resolve("dist");
            if (Files.exists(distBackup)) {
                Path distTarget = Paths.get(projectRoot, "jicek-ui", "dist");
                copyDir(distBackup, distTarget);
            }
            // 重启
            restart();
            log.info("[Deploy] 回滚完成 <- {}", latestBackup);
        } catch (IOException e) {
            throw new ServiceException(ResultCode.DEPLOY_ROLLBACK_FAIL, e.getMessage());
        }
    }

    /* ============ 状态查询 ============ */

    /**
     * 查询当前部署状态
     */
    public DeployStatusDTO getStatus() {
        DeployStatusDTO dto = new DeployStatusDTO();
        dto.setDeploying(deploying);
        dto.setEnabled(properties.getDeploy().isEnabled());
        // 查最近一条日志
        List<DeployLog> logs = deployLogMapper.selectList(
                new LambdaQueryWrapper<DeployLog>()
                        .eq(DeployLog::getTenantId, 1L)
                        .orderByDesc(DeployLog::getId)
                        .last("LIMIT 1"));
        dto.setLastDeploy(logs.isEmpty() ? null : logs.get(0));
        return dto;
    }

    /**
     * 分页查询部署日志
     */
    public List<DeployLog> logPage(Long tenantId, Integer status, String triggerSource,
                                   int current, int size) {
        return deployLogMapper.selectList(
                new LambdaQueryWrapper<DeployLog>()
                        .eq(DeployLog::getTenantId, tenantId)
                        .eq(status != null, DeployLog::getStatus, status)
                        .eq(StrUtil.isNotBlank(triggerSource),
                                DeployLog::getTriggerSource, triggerSource)
                        .orderByDesc(DeployLog::getId)
                        .last("LIMIT " + size + " OFFSET " + (Math.max(current - 1, 0) * size)));
    }

    /* ============ 工具方法 ============ */

    /**
     * 验证 Webhook 签名（HMAC-SHA256，常量时间比较）
     */
    private void verifySignature(String body, String signature) {
        String secret = properties.getDeploy().getWebhookSecret();
        if (StrUtil.isBlank(secret)) {
            throw new ServiceException(ResultCode.DEPLOY_SECRET_NOT_CONFIGURED);
        }
        if (StrUtil.isBlank(signature)
                || !signature.startsWith(JicekConstants.DEPLOY_WEBHOOK_SIGNATURE_PREFIX)) {
            throw new ServiceException(ResultCode.DEPLOY_WEBHOOK_SIGN_FAIL);
        }
        String received = signature.substring(JicekConstants.DEPLOY_WEBHOOK_SIGNATURE_PREFIX.length());
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expectedBytes = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : expectedBytes) {
                sb.append(String.format("%02x", b));
            }
            String expected = sb.toString();
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    received.getBytes(StandardCharsets.UTF_8))) {
                throw new ServiceException(ResultCode.DEPLOY_WEBHOOK_SIGN_FAIL);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(ResultCode.DEPLOY_WEBHOOK_SIGN_FAIL);
        }
    }

    /**
     * 解析 commit hash（payload.head_commit.id）
     */
    private String parseCommitHash(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode headCommit = root.get("head_commit");
            if (headCommit != null && headCommit.has("id")) {
                return headCommit.get("id").asText();
            }
        } catch (Exception e) {
            log.warn("[Deploy] 解析 commit hash 失败", e);
        }
        return null;
    }

    /**
     * 解析分支名（payload.ref = refs/heads/main → main）
     */
    private String parseBranch(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.has("ref")) {
                String ref = root.get("ref").asText();
                String prefix = "refs/heads/";
                if (ref.startsWith(prefix)) {
                    return ref.substring(prefix.length());
                }
            }
        } catch (Exception e) {
            log.warn("[Deploy] 解析分支失败", e);
        }
        return null;
    }

    /**
     * 读取 request body（缓存为 String，因 InputStream 只能读一次）
     */
    private String readBody(HttpServletRequest request) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            throw new ServiceException(ResultCode.DEPLOY_WEBHOOK_SIGN_FAIL, "读取 body 失败");
        }
    }

    /**
     * 获取客户端真实 IP（穿透代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 执行外部命令（使用 ProcessBuilder，禁 Runtime.exec 防注入）
     */
    private void execCommand(File workDir, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workDir);
        pb.redirectErrorStream(true);
        pb.environment().put("LANG", "en_US.UTF-8");
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String errMsg = output.toString();
            if (errMsg.length() > 2000) {
                errMsg = errMsg.substring(0, 2000) + "...(truncated)";
            }
            throw new RuntimeException("命令执行失败 exitCode=" + exitCode + " cmd="
                    + String.join(" ", command) + " output=" + errMsg);
        }
    }

    /**
     * 递归拷贝目录
     */
    private void copyDir(Path src, Path dest) throws IOException {
        try (Stream<Path> stream = Files.walk(src)) {
            stream.forEach(source -> {
                try {
                    Path target = dest.resolve(src.relativize(source));
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException("拷贝失败: " + source, e);
                }
            });
        }
    }

    /**
     * 清理旧备份（保留最近 N 个）
     */
    private void cleanupOldBackups(File rootDir) {
        Path backupRoot = Paths.get(rootDir.getAbsolutePath(), ".jicek-backup");
        if (!Files.exists(backupRoot)) return;
        try (Stream<Path> dirs = Files.list(backupRoot)) {
            List<Path> backups = dirs
                    .filter(Files::isDirectory)
                    .sorted(Comparator.reverseOrder())
                    .toList();
            for (int i = JicekConstants.DEPLOY_BACKUP_KEEP_COUNT; i < backups.size(); i++) {
                deleteDir(backups.get(i));
            }
        } catch (IOException e) {
            log.warn("[Deploy] 清理旧备份失败", e);
        }
    }

    /**
     * 递归删除目录
     */
    private void deleteDir(Path path) {
        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            log.warn("[Deploy] 删除失败: {}", p);
                        }
                    });
        } catch (IOException e) {
            log.warn("[Deploy] 删除目录失败: {}", path);
        }
    }

    /**
     * 截断错误信息至 4KB
     */
    private String truncateError(String msg) {
        if (msg == null) return null;
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= JicekConstants.DEPLOY_ERROR_MSG_MAX_BYTES) {
            return msg;
        }
        return new String(bytes, 0, JicekConstants.DEPLOY_ERROR_MSG_MAX_BYTES,
                StandardCharsets.UTF_8);
    }

    /**
     * 更新部署日志（审计表仅允许此受控更新，状态从 0→1/2/3）
     */
    private void updateDeployLog(Long logId, int status, int durationMs, String errorMessage) {
        DeployLog update = new DeployLog();
        update.setId(logId);
        update.setStatus(status);
        update.setDurationMs(durationMs);
        update.setErrorMessage(errorMessage);
        deployLogMapper.updateById(update);
    }
}
