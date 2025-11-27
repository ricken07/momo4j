package com.rickenbazolo.momo4j.airtel.congo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rickenbazolo.momo4j.core.http.MoMoHttpClient;
import com.rickenbazolo.momo4j.core.exception.MoMoHttpException;
import com.rickenbazolo.momo4j.core.http.MoMoHttpRequest;
import com.rickenbazolo.momo4j.airtel.congo.MtnCongoConfig;
import com.rickenbazolo.momo4j.airtel.congo.collection.model.MtnRequestToPay;
import com.rickenbazolo.momo4j.airtel.congo.model.MtnTransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Service for handling MTN Mobile Money transaction operations.
 * <p>
 * This service handles Request to Pay initiation and transaction status queries.
 * </p>
 * @author Ricken Bazolo
 * @since 0.1.0
 */
public class MtnTransactionService {

    private static final Logger logger = LoggerFactory.getLogger(MtnTransactionService.class);
    private static final String REQUEST_TO_PAY_ENDPOINT = "collection/v1_0/requesttopay";
    private static final String REQUEST_TO_PAY_STATUS_ENDPOINT = "collection/v1_0/requesttopay/";

    private final MtnCongoConfig config;
    private final MoMoHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new MtnTransactionService.
     *
     * @param config     the MTN configuration
     * @param httpClient the HTTP client
     */
    public MtnTransactionService(MtnCongoConfig config, MoMoHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Initiate a Request to Pay transaction.
     * <p>
     * This sends a payment request to the customer's mobile phone.
     * The customer will receive a USSD push notification to approve the payment.
     * </p>
     *
     * @param mtnRequest   the request to pay details
     * @param accessToken  the access token
     * @return the external ID of the initiated transaction
     * @throws MoMoHttpException if the request fails
     */
    public String requestToPay(MtnRequestToPay mtnRequest, String accessToken) throws MoMoHttpException {
        logger.debug("Initiating Request to Pay for external ID: {}", mtnRequest.getExternalId());

        var httpRequest = buildRequestToPayRequest(mtnRequest, accessToken);
        var response = httpClient.execute(httpRequest);

        // MTN returns 202 Accepted with nobody on success
        if (response.statusCode() == 202) {
            logger.debug("Request to Pay initiated successfully: {}", mtnRequest.getExternalId());
            return mtnRequest.getExternalId();
        }

        // Handle errors
        logger.error("Request to Pay failed: HTTP {}", response.statusCode());
        byte[] body = response.body().orElse(new byte[0]);
        throw handleRequestToPayError(response.statusCode(), new String(body, StandardCharsets.UTF_8));
    }

    /**
     * Get the status of a transaction.
     * <p>
     * Query the status of a previously initiated Request to Pay transaction.
     * </p>
     *
     * @param externalId  the external ID of the transaction
     * @param accessToken the access token
     * @return the transaction status
     * @throws MoMoHttpException if the request fails
     */
    public MtnTransactionStatus getTransactionStatus(String externalId, String accessToken) throws MoMoHttpException {
        logger.debug("Querying transaction status for: {}", externalId);

        var httpRequest = buildStatusRequest(externalId, accessToken);
        var response = httpClient.execute(httpRequest);

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            logger.error("Failed to get transaction status: HTTP {}", response.statusCode());
            throw new MoMoHttpException(
                "Failed to get transaction status: HTTP " + response.statusCode()
            );
        }

        try {
            byte[] bodyBytes = response.body().orElseThrow(() ->
                new MoMoHttpException("Empty response body"));
            var status = objectMapper.readValue(bodyBytes, MtnTransactionStatus.class);
            logger.debug("Transaction status for {}: {}", externalId, status.getStatus());
            return status;

        } catch (Exception e) {
            logger.error("Failed to parse transaction status response", e);
            throw new MoMoHttpException("Failed to parse transaction status response", e);
        }
    }

    /**
     * Build the HTTP request for Request to Pay.
     *
     * @param mtnRequest  the request to pay details
     * @param accessToken the access token
     * @return the HTTP request
     */
    private MoMoHttpRequest buildRequestToPayRequest(MtnRequestToPay mtnRequest, String accessToken) {
        try {
            String body = objectMapper.writeValueAsString(mtnRequest);

            var requestBuilder = MoMoHttpRequest.builder()
                .url(config.getBaseUrl() + REQUEST_TO_PAY_ENDPOINT)
                .method("POST")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Reference-Id", mtnRequest.getExternalId())
                .header("X-Target-Environment", config.getEnvironment())
                .header("Ocp-Apim-Subscription-Key", config.getSubscriptionKey())
                .header("Content-Type", "application/json")
                .body(body);

            // Add callback URL if configured
            if (config.getCallbackUrl() != null) {
                requestBuilder.header("X-Callback-Url", config.getCallbackUrl());
            }

            return requestBuilder.build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize request to pay", e);
        }
    }

    /**
     * Build the HTTP request for getting transaction status.
     *
     * @param externalId  the external ID
     * @param accessToken the access token
     * @return the HTTP request
     */
    private MoMoHttpRequest buildStatusRequest(String externalId, String accessToken) {
        return MoMoHttpRequest.builder()
            .url(config.getBaseUrl() + REQUEST_TO_PAY_STATUS_ENDPOINT + externalId)
            .method("GET")
            .header("Authorization", "Bearer " + accessToken)
            .header("X-Target-Environment", config.getEnvironment())
            .header("Ocp-Apim-Subscription-Key", config.getSubscriptionKey())
            .header("Content-Type", "application/json")
            .build();
    }

    /**
     * Handle Request to Pay errors based on HTTP status code.
     *
     * @param statusCode the HTTP status code
     * @param body       the response body
     * @return a MoMoHttpException
     */
    private MoMoHttpException handleRequestToPayError(int statusCode, String body) {
        return switch (statusCode) {
            case 400 -> new MoMoHttpException(
                "Bad Request: Invalid parameters. MTN API error with message %s".formatted(body)
            );
            case 401 -> new MoMoHttpException(
                "Unauthorized: MTN API error with message %s".formatted(body)
            );
            case 409 -> new MoMoHttpException(
                "Conflict: Duplicate external ID. MTN API error with message %s".formatted(body)
            );
            case 500 -> new MoMoHttpException(
                "Internal Server Error: MTN API error with message %s".formatted(body)
            );
            default -> new MoMoHttpException(
                "Request to Pay failed: status %s with message %s".formatted(statusCode, body)
            );
        };
    }
}
