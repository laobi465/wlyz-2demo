package com.jicek.sdk;

import java.util.Map;

/**
 * 极简 JSON 工具（无第三方依赖）
 * 作者: 极策k  日期: 2026-07-21
 *
 * 仅满足 SDK 内部使用：序列化 Map → JSON，解析响应 {code,msg,data}
 * 生产环境可替换为 Jackson/Gson
 */
public class JsonUtil {

    /** Map → JSON 字符串 */
    public static String toJson(Map<String, ?> map) {
        if (map == null) return "null";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escape(e.getKey())).append("\":");
            sb.append(toJsonValue(e.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private static String toJsonValue(Object v) {
        if (v == null) return "null";
        if (v instanceof String) return "\"" + escape((String) v) + "\"";
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        return "\"" + escape(v.toString()) + "\"";
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * 解析服务端响应：{ "code":200, "msg":"...", "data":{...} }
     * 失败抛 JicekException
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseResponse(String json) {
        // 极简解析：直接转为 Map（生产建议换 Jackson）
        try {
            Object parsed = new SimpleJsonParser(json).parse();
            if (!(parsed instanceof Map)) {
                throw new JicekException(500, "响应格式错误：非 JSON 对象");
            }
            Map<String, Object> root = (Map<String, Object>) parsed;
            Object codeObj = root.get("code");
            int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : 0;
            if (code != 200) {
                String msg = (String) root.getOrDefault("msg", "未知错误");
                throw new JicekException(code, msg);
            }
            Object data = root.get("data");
            if (data instanceof Map) {
                return (Map<String, Object>) data;
            }
            return root;
        } catch (JicekException e) {
            throw e;
        } catch (Exception e) {
            throw new JicekException(500, "响应解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 极简递归下降 JSON 解析器（仅支持 Object/Array/String/Number/Boolean/null）
     */
    static class SimpleJsonParser {
        private final String s;
        private int pos;

        SimpleJsonParser(String s) {
            this.s = s;
            this.pos = 0;
        }

        Object parse() {
            skipWs();
            Object v = parseValue();
            skipWs();
            return v;
        }

        private Object parseValue() {
            skipWs();
            if (pos >= s.length()) throw new RuntimeException("JSON 解析到末尾");
            char c = s.charAt(pos);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBool();
            if (c == 'n') return parseNull();
            return parseNumber();
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            pos++; // skip {
            skipWs();
            if (pos < s.length() && s.charAt(pos) == '}') { pos++; return m; }
            while (true) {
                skipWs();
                String key = parseString();
                skipWs();
                if (pos >= s.length() || s.charAt(pos) != ':') throw new RuntimeException("JSON 对象缺冒号");
                pos++;
                Object value = parseValue();
                m.put(key, value);
                skipWs();
                if (pos >= s.length()) throw new RuntimeException("JSON 对象未闭合");
                char next = s.charAt(pos);
                if (next == ',') { pos++; continue; }
                if (next == '}') { pos++; break; }
                throw new RuntimeException("JSON 对象格式错误: " + next);
            }
            return m;
        }

        private java.util.List<Object> parseArray() {
            java.util.List<Object> list = new java.util.ArrayList<>();
            pos++; // skip [
            skipWs();
            if (pos < s.length() && s.charAt(pos) == ']') { pos++; return list; }
            while (true) {
                list.add(parseValue());
                skipWs();
                if (pos >= s.length()) throw new RuntimeException("JSON 数组未闭合");
                char next = s.charAt(pos);
                if (next == ',') { pos++; continue; }
                if (next == ']') { pos++; break; }
                throw new RuntimeException("JSON 数组格式错误: " + next);
            }
            return list;
        }

        private String parseString() {
            if (s.charAt(pos) != '"') throw new RuntimeException("JSON 字符串缺引号");
            pos++;
            StringBuilder sb = new StringBuilder();
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    if (pos >= s.length()) throw new RuntimeException("JSON 转义异常");
                    char e = s.charAt(pos++);
                    switch (e) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (pos + 4 > s.length()) throw new RuntimeException("JSON \\u 转义异常");
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                            pos += 4;
                            break;
                        default: throw new RuntimeException("未知 JSON 转义: \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new RuntimeException("JSON 字符串未闭合");
        }

        private Number parseNumber() {
            int start = pos;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                    pos++;
                } else {
                    break;
                }
            }
            String num = s.substring(start, pos);
            if (num.contains(".") || num.contains("e") || num.contains("E")) {
                return Double.parseDouble(num);
            }
            try {
                return Long.parseLong(num);
            } catch (NumberFormatException ex) {
                return Double.parseDouble(num);
            }
        }

        private Boolean parseBool() {
            if (s.startsWith("true", pos)) { pos += 4; return true; }
            if (s.startsWith("false", pos)) { pos += 5; return false; }
            throw new RuntimeException("JSON 布尔值解析错误");
        }

        private Object parseNull() {
            if (s.startsWith("null", pos)) { pos += 4; return null; }
            throw new RuntimeException("JSON null 解析错误");
        }

        private void skipWs() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }
    }
}
