package com.rickenbazolo.momo4j.http.client;

import com.rickenbazolo.momo4j.core.http.MoMoHttpClient;
import com.rickenbazolo.momo4j.core.exception.MoMoHttpException;
import com.rickenbazolo.momo4j.core.http.MoMoHttpRequest;
import com.rickenbazolo.momo4j.core.http.MoMoHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Default HTTP client implementation using java.net.http.HttpClient.
 * <p>
 * This implementation requires Java 11+ and provides zero external dependencies.
 * It supports both synchronous and asynchronous HTTP operations with HTTP/2 by default.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * var httpClient = DefaultMoMoHttpClient.builder()
 *     .connectTimeout(5000)
 *     .requestTimeout(30000)
 *     .build();
 *
 * var request = MoMoHttpRequest.builder()
 *     .url("https://api.example.com/endpoint")
 *     .method("POST")
 *     .header("Content-Type", "application/json")
 *     .body("{\"key\":\"value\"}")
 *     .build();
 *
 * var response = httpClient.execute(request);
 * }</pre>
 *

 * @author Ricken Bazolo * @since 0.1.0
 */
public class DefaultMoMoHttpClient implements MoMoHttpClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultMoMoHttpClient.class);

    private final HttpClient delegate;
    private final Duration requestTimeout;

    private DefaultMoMoHttpClient(Builder builder) {
        this.requestTimeout = Duration.ofMillis(builder.requestTimeout);

        var clientBuilder = HttpClient.newBuilder()
            .version(builder.httpVersion)
            .connectTimeout(Duration.ofMillis(builder.connectTimeout));

        if (builder.followRedirects != null) {
            clientBuilder.followRedirects(builder.followRedirects);
        }

        this.delegate = clientBuilder.build();

        log.debug("DefaultMoMoHttpClient initialized with connectTimeout={}ms, requestTimeout={}ms, httpVersion={}",
            builder.connectTimeout, builder.requestTimeout, builder.httpVersion);
    }

    @Override
    public MoMoHttpResponse execute(MoMoHttpRequest momoRequest) throws MoMoHttpException {
        log.debug("Executing HTTP Mobile Money Request: {} {}", momoRequest.method(), momoRequest.url());

        try {
            var request = toHttpRequest(momoRequest);
            var response = delegate.send(request, BodyHandlers.ofByteArray());

            var httpResponse = toHttpResponse(response);

            log.debug("HTTP momoRequest completed: {} {} -> {}", momoRequest.method(), momoRequest.url(), httpResponse.statusCode());

            return httpResponse;

        } catch (IOException e) {
            log.error("HTTP momoRequest failed: {} {}", momoRequest.method(), momoRequest.url(), e);
            throw new MoMoHttpException("HTTP momoRequest failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("HTTP momoRequest interrupted: {} {}", momoRequest.method(), momoRequest.url(), e);
            throw new MoMoHttpException("HTTP momoRequest interrupted", e);
        } catch (Exception e) {
            log.error("Unexpected error during HTTP momoRequest: {} {}", momoRequest.method(), momoRequest.url(), e);
            throw new MoMoHttpException("Unexpected error: " + e.getMessage(), e);
        }
    }

    @Override
    public CompletableFuture<MoMoHttpResponse> executeAsync(MoMoHttpRequest momoRequest) {
        log.debug("Executing async HTTP Mobile Money Request: {} {}", momoRequest.method(), momoRequest.url());

        try {
            var request = toHttpRequest(momoRequest);

            return delegate.sendAsync(request, BodyHandlers.ofByteArray())
                .thenApply(javaResponse -> {
                    var response = toHttpResponse(javaResponse);
                    log.debug("Async HTTP momoRequest completed: {} {} -> {}",
                        momoRequest.method(), momoRequest.url(), response.statusCode());
                    return response;
                })
                .exceptionally(throwable -> {
                    log.error("Async HTTP momoRequest failed: {} {}", momoRequest.method(), momoRequest.url(), throwable);
                    throw new RuntimeException("Async HTTP momoRequest failed", throwable);
                });

        } catch (Exception e) {
            log.error("Failed to initiate async HTTP momoRequest: {} {}", momoRequest.method(), momoRequest.url(), e);
            return CompletableFuture.failedFuture(new MoMoHttpException("Failed to initiate momoRequest", e));
        }
    }

    @Override
    public void close() {
        // java.net.http.MoMoHttpClient does not require explicit closing
        // Connection pooling is managed automatically
        log.debug("DefaultMoMoHttpClient closed (no-op for java.net.http.MoMoHttpClient)");
    }

    /**
     * Convert MoMoHttpRequest to java.net.http.HttpRequest.
     */
    private HttpRequest toHttpRequest(MoMoHttpRequest momoRequest) {
        var builder = HttpRequest.newBuilder()
            .uri(URI.create(momoRequest.url()))
            .timeout(requestTimeout);

        momoRequest.headers().forEach(builder::header);

        var bodyPublisher = momoRequest.body()
            .map(BodyPublishers::ofByteArray)
            .orElse(BodyPublishers.noBody());

        builder.method(momoRequest.method(), bodyPublisher);

        return builder.build();
    }

    /**
     * Convert java.net.http.HttpResponse to our MoMoHttpResponse.
     */
    private MoMoHttpResponse toHttpResponse(HttpResponse<byte[]> response) {
        var responseBuilder = MoMoHttpResponse.builder()
            .statusCode(response.statusCode())
            .body(response.body());

        response.headers().map().forEach((name, values) -> {
            if (!values.isEmpty()) {
                responseBuilder.header(name, values.get(0));
            }
        });

        return responseBuilder.build();
    }

    /**
     * Creates a new builder for DefaultMoMoHttpClient.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for DefaultMoMoHttpClient.
     */
    public static class Builder {
        private int connectTimeout = 30000; // 30 seconds
        private int requestTimeout = 60000; // 60 seconds
        private HttpClient.Version httpVersion = HttpClient.Version.HTTP_2;
        private HttpClient.Redirect followRedirects = null;

        /**
         * Set the connection timeout in milliseconds.
         *
         * @param timeout the timeout in milliseconds
         * @return this builder
         */
        public Builder connectTimeout(int timeout) {
            if (timeout <= 0) {
                throw new IllegalArgumentException("Connect timeout must be positive");
            }
            this.connectTimeout = timeout;
            return this;
        }

        /**
         * Set the request timeout in milliseconds.
         *
         * @param timeout the timeout in milliseconds
         * @return this builder
         */
        public Builder requestTimeout(int timeout) {
            if (timeout <= 0) {
                throw new IllegalArgumentException("Request timeout must be positive");
            }
            this.requestTimeout = timeout;
            return this;
        }

        /**
         * Set the HTTP version to use.
         *
         * @param version the HTTP version (HTTP_1_1 or HTTP_2)
         * @return this builder
         */
        public Builder httpVersion(HttpClient.Version version) {
            this.httpVersion = version;
            return this;
        }

        /**
         * Set the redirect policy.
         *
         * @param followRedirects the redirect policy
         * @return this builder
         */
        public Builder followRedirects(HttpClient.Redirect followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        /**
         * Build the DefaultMoMoHttpClient instance.
         *
         * @return a new DefaultMoMoHttpClient instance
         */
        public DefaultMoMoHttpClient build() {
            return new DefaultMoMoHttpClient(this);
        }
    }
}
