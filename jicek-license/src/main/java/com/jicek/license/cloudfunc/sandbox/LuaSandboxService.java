package com.jicek.license.cloudfunc.sandbox;

import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.BaseLib;
import org.luaj.vm2.lib.MathLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/**
 * Lua 沙箱执行引擎
 * 作者: 极策k  日期: 2026-07-22
 *
 * 安全设计（铁律 04/06/13）：
 * 1. 全局表裁剪：禁用 os/io/loadfile/dofile/require/debug/package
 *    保留：string/table/math/number（核心库）
 * 2. 超时强制中断：Future.get(timeoutMs)，超时后 future.cancel(true) 中断线程
 * 3. 内存限制：JVM -Xmx 全局限制 + 输出大小硬截断（maxOutputKb）
 * 4. 输入注入：通过 jicek.input 全局变量传入（字符串）
 * 5. 输出契约：Lua 代码必须 return 值（string/number/boolean/table），序列化为 JSON 字符串
 *
 * 调用约定：
 *   Lua 代码访问输入：local data = jicek.input
 *   Lua 代码返回值：return result（任意 Lua 值，会被序列化为 JSON）
 *
 * 异常映射：
 *   LuaError → CF_RUNTIME_ERROR / CF_COMPILE_FAIL
 *   TimeoutException → CF_TIMEOUT
 *   InterruptedException → CF_TIMEOUT（线程被中断）
 *   OutOfMemoryError → CF_MEMORY_LIMIT
 */
@Slf4j
@Service
public class LuaSandboxService {

    /**
     * 线程池：执行 Lua 代码用，独立线程池便于隔离与监控
     * 核心线程数 4，最大 16，队列 64，超出后由调用方处理（CallerRuns 退化为同步）
     */
    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(
            4, 16,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(64),
            r -> {
                Thread t = new Thread(r, "jicek-lua-sandbox");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * 执行 Lua 代码
     *
     * @param code        Lua 源代码
     * @param input       输入字符串（JSON 或任意文本），可为 null
     * @param timeoutMs   超时毫秒
     * @param maxOutputKb 输出大小上限（KB）
     * @return 序列化后的输出字符串
     * @throws ServiceException 编译失败/运行时错误/超时/内存超限/输出超限
     */
    public String execute(String code, String input, int timeoutMs, int maxOutputKb) {
        // 1. 预编译校验（同步执行，快速失败）
        Globals previewGlobals = buildSandboxGlobals();
        try {
            previewGlobals.load(code, "jicek_function").checkfunction();
        } catch (LuaError e) {
            log.warn("Lua 编译失败: {}", e.getMessage());
            throw new ServiceException(ResultCode.CF_COMPILE_FAIL, truncateError(e.getMessage()));
        }

        // 2. 异步执行（受超时控制）
        Future<String> future = EXECUTOR.submit(() -> doExecute(code, input, maxOutputKb));
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ServiceException(ResultCode.CF_TIMEOUT, "执行超过 " + timeoutMs + "ms 被强制中断");
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new ServiceException(ResultCode.CF_TIMEOUT, "执行被中断");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ServiceException) {
                throw (ServiceException) cause;
            }
            if (cause instanceof OutOfMemoryError) {
                throw new ServiceException(ResultCode.CF_MEMORY_LIMIT, "执行内存超限");
            }
            log.error("Lua 执行异常", cause);
            throw new ServiceException(ResultCode.CF_RUNTIME_ERROR, truncateError(cause.getMessage()));
        }
    }

    /**
     * 实际执行 Lua 代码（在沙箱线程中运行）
     */
    private String doExecute(String code, String input, int maxOutputKb) {
        Globals globals = buildSandboxGlobals();

        // 注入 jicek.input 全局变量
        LuaTable jicekTable = new LuaTable();
        jicekTable.set("input", LuaValue.valueOf(input == null ? "" : input));
        globals.set("jicek", jicekTable);

        LuaValue func;
        try {
            func = globals.load(code, "jicek_function").checkfunction();
        } catch (LuaError e) {
            throw new ServiceException(ResultCode.CF_COMPILE_FAIL, truncateError(e.getMessage()));
        }

        LuaValue result;
        try {
            result = func.call();
        } catch (LuaError e) {
            throw new ServiceException(ResultCode.CF_RUNTIME_ERROR, truncateError(e.getMessage()));
        } catch (Exception e) {
            throw new ServiceException(ResultCode.CF_RUNTIME_ERROR, truncateError(e.getMessage()));
        }

        // 序列化返回值
        if (result == null || result.isnil()) {
            return "null";
        }
        String json = luaValueToJson(result);
        int outputBytes = json.getBytes(StandardCharsets.UTF_8).length;
        int maxBytes = maxOutputKb * 1024;
        if (outputBytes > maxBytes) {
            throw new ServiceException(ResultCode.CF_OUTPUT_TOO_LARGE,
                    "输出 " + outputBytes + "B 超过上限 " + maxBytes + "B");
        }
        return json;
    }

    /**
     * 构建沙箱全局表
     * 仅保留：base（部分）/ string / table / math
     * 禁用：os / io / loadfile / dofile / require / debug / package / load
     */
    private Globals buildSandboxGlobals() {
        Globals globals = new Globals();
        // 1. 先安装 LuaC 编译器（globals.load() 依赖它编译用户代码）
        LuaC.install(globals);
        // 2. 加载核心库
        globals.load(new BaseLib());
        globals.load(new MathLib());
        globals.load(new StringLib());
        globals.load(new TableLib());

        // 3. 禁用危险函数：os / io / loadfile / dofile / require / debug / package / load
        //    load 函数直接禁用（避免动态编译，所有代码必须在源码顶层）
        globals.set("os", LuaValue.NIL);
        globals.set("io", LuaValue.NIL);
        globals.set("loadfile", LuaValue.NIL);
        globals.set("dofile", LuaValue.NIL);
        globals.set("require", LuaValue.NIL);
        globals.set("debug", LuaValue.NIL);
        globals.set("package", LuaValue.NIL);
        globals.set("load", LuaValue.NIL);

        return globals;
    }

    /**
     * 将 Lua 值序列化为 JSON 字符串
     * 支持：nil / boolean / number / string / table（一维或多维）
     * 不支持：function / userdata / thread
     */
    private String luaValueToJson(LuaValue value) {
        StringBuilder sb = new StringBuilder();
        appendJson(sb, value);
        return sb.toString();
    }

    private void appendJson(StringBuilder sb, LuaValue value) {
        if (value == null || value.isnil()) {
            sb.append("null");
        } else if (value.isboolean()) {
            sb.append(value.toboolean() ? "true" : "false");
        } else if (value.isnumber()) {
            double d = value.todouble();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                sb.append("null");
            } else if (d == Math.rint(d) && !Double.isInfinite(d)) {
                sb.append((long) d);
            } else {
                sb.append(d);
            }
        } else if (value.isstring()) {
            appendString(sb, value.tojstring());
        } else if (value.istable()) {
            appendTable(sb, value.checktable());
        } else {
            // function/userdata/thread 不支持序列化
            sb.append("null");
        }
    }

    private void appendString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    /**
     * 序列化 Lua table 为 JSON
     * 判断策略：若所有 key 均为正整数且从 1 开始连续，序列化为 JSON 数组；否则为对象
     */
    private void appendTable(StringBuilder sb, LuaTable table) {
        // 先判断是否为纯数组（key 为 1..n）
        LuaValue[] keys = table.keys();
        if (keys.length == 0) {
            sb.append("{}");
            return;
        }

        boolean isArray = true;
        int maxIndex = 0;
        for (LuaValue k : keys) {
            if (!k.isint()) {
                isArray = false;
                break;
            }
            int idx = k.toint();
            if (idx < 1) {
                isArray = false;
                break;
            }
            if (idx > maxIndex) maxIndex = idx;
        }
        // 连续性检查：1..maxIndex 必须都存在
        if (isArray) {
            for (int i = 1; i <= maxIndex; i++) {
                if (table.get(i).isnil()) {
                    isArray = false;
                    break;
                }
            }
        }

        if (isArray) {
            sb.append('[');
            for (int i = 1; i <= maxIndex; i++) {
                if (i > 1) sb.append(',');
                appendJson(sb, table.get(i));
            }
            sb.append(']');
        } else {
            sb.append('{');
            boolean first = true;
            for (LuaValue k : keys) {
                if (!first) sb.append(',');
                first = false;
                appendString(sb, k.tojstring());
                sb.append(':');
                appendJson(sb, table.get(k));
            }
            sb.append('}');
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
