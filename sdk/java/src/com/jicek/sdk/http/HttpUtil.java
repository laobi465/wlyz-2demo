package com.jicek.sdk.http;

import com.jicek.sdk.JicekException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * HTTP 通信（JDK 17+ HttpClient，无第三方依赖）
 * 作者: 极策k  日期: 2026-07-21
 */
public class HttpUtil {

    private final HttpClient httpClient;

    public HttpUtil(int connectTimeoutMs) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
    }

    /**
     * 发送 JSON 请求
     *
     * @param url     完整 URL
     * @param method  GET / POST
     * @param body    请求体（GET 传 null）
     * @param headers 请求头
     * @return 响应体字符串
     */
    public String request(String url, String method, String body, Map<String, String> headers) {
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30));

            for (Map.Entry<String, String> e : headers.entrySet()) {
                reqBuilder.header(e.getKey(), e.getValue());
            }
            reqBuilder.header("Content-Type", "application/json; charset=UTF-8");

            if ("GET".equalsIgnoreCase(method)) {
                reqBuilder.GET();
            } else if ("POST".equalsIgnoreCase(method)) {
                reqBuilder.POST(HttpRequest.BodyPublishers.ofString(
                        body == null ? "" : body, StandardCharsets.UTF_8));
            } else {
                throw new JicekException(500, "不支持的 HTTP 方法: " + method);
            }

            HttpResponse<String> resp = httpClient.send(
                    reqBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (resp.statusCode() != 200) {
                throw new JicekException(resp.statusCode(),
                        "HTTP 请求失败: " + resp.statusCode() + " " + resp.body());
            }
            return resp.body();
        } catch (JicekException e) {
            throw e;
        } catch (Exception e) {
            throw new JicekException(500, "HTTP 请求异常: " + e.getMessage(), e);
        }
    }
}
