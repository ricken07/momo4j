package com.rickenbazolo.momo4j.core.http;

import com.rickenbazolo.momo4j.core.exception.MoMoHttpException;

import java.util.concurrent.CompletableFuture;

/**
 * Core HTTP client abstraction for Mobile Money API requests.
 * This interface allows pluggable HTTP client implementations.
 *
 * @author Ricken Bazolo
 * @since 0.1.0
 */
public interface MoMoHttpClient {

    /**
     * Execute an HTTP request synchronously.
     *
     * @param request the HTTP request to execute
     * @return the HTTP response
     * @throws MoMoHttpException if an error occurs during request execution
     */
    MoMoHttpResponse execute(MoMoHttpRequest request) throws MoMoHttpException;

    /**
     * Execute an HTTP request asynchronously.
     *
     * @param request the HTTP request to execute
     * @return a CompletableFuture containing the HTTP response
     */
    CompletableFuture<MoMoHttpResponse> executeAsync(MoMoHttpRequest request);

    /**
     * Close the HTTP client and release any resources.
     */
    void close();
}
