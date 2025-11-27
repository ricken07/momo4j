package com.rickenbazolo.momo4j.core;

import com.rickenbazolo.momo4j.core.http.MoMoHttpClient;

/**
 * Base interface for all Mobile Money operator clients.
 * Each operator-specific module will implement this interface.
 *
 * @param <T> the configuration type for this client
 * @author Ricken Bazolo
 * @since 0.1.0
 */
public interface MobileMoneyClient<T extends MobileMoneyConfig> extends AutoCloseable {

    /**
     * Get the configuration used by this client.
     *
     * @return the client configuration
     */
    T getConfig();

    /**
     * Get the underlying HTTP client.
     *
     * @return the HTTP client
     */
    MoMoHttpClient getHttpClient();

    /**
     * Close the client and release resources.
     */
    @Override
    void close();
}
