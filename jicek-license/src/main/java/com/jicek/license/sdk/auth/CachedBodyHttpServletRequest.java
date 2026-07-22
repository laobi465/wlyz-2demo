package com.jicek.license.sdk.auth;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 请求体缓存包装类
 * 作者: 极策k  日期: 2026-07-22
 *
 * 问题：HTTP 请求的 InputStream 只能读一次。SDK 签名校验需在 Filter 中读取 body
 * 计算 SHA-256，之后 Spring MVC 的 @RequestBody 还需再次读取 body。
 *
 * 方案：在构造时将 body 读取为 byte[] 缓存，getInputStream / getReader 返回缓存的副本，
 * 使得 body 可被多次读取。
 *
 * 仅用于 /api/sdk/** 路径（SdkAuthFilter 内包装），不影响其他请求。
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        // 读取并缓存完整 body
        this.cachedBody = request.getInputStream().readAllBytes();
    }

    /** 返回缓存的 body 字节数组（用于签名校验计算 SHA-256） */
    public byte[] getCachedBody() {
        return cachedBody;
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(cachedBody), StandardCharsets.UTF_8));
    }

    /** 内部 ServletInputStream 实现，从缓存 byte[] 读取 */
    private static class CachedServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream delegate;

        CachedServletInputStream(byte[] body) {
            this.delegate = new ByteArrayInputStream(body);
        }

        @Override
        public boolean isFinished() {
            return delegate.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("CachedServletInputStream 不支持异步读取");
        }

        @Override
        public int read() {
            return delegate.read();
        }
    }
}
