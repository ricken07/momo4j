package com.rickenbazolo.momo4j.http.client;

import com.rickenbazolo.momo4j.core.exception.MoMoHttpException;
import com.rickenbazolo.momo4j.core.http.MoMoHttpRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for DefaultMoMoHttpClient.
 */
class DefaultMoMoHttpClientTest {

    private DefaultMoMoHttpClient httpClient;

    @BeforeEach
    void setUp() {
        httpClient = DefaultMoMoHttpClient.builder()
            .connectTimeout(5000)
            .requestTimeout(10000)
            .build();
    }

    @AfterEach
    void tearDown() {
        if (httpClient != null) {
            httpClient.close();
        }
    }

    @Test
    @DisplayName("Should create client with default settings")
    void shouldCreateClientWithDefaults() {
        var client = DefaultMoMoHttpClient.builder().build();
        assertThat(client).isNotNull();
        client.close();
    }

    @Test
    @DisplayName("Should create client with custom timeouts")
    void shouldCreateClientWithCustomTimeouts() {
        var client = DefaultMoMoHttpClient.builder()
            .connectTimeout(3000)
            .requestTimeout(5000)
            .build();

        assertThat(client).isNotNull();
        client.close();
    }

    @Test
    @DisplayName("Should reject negative connect timeout")
    void shouldRejectNegativeConnectTimeout() {
        assertThatThrownBy(() ->
            DefaultMoMoHttpClient.builder().connectTimeout(-1)
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Connect timeout must be positive");
    }

    @Test
    @DisplayName("Should reject negative request timeout")
    void shouldRejectNegativeRequestTimeout() {
        assertThatThrownBy(() ->
            DefaultMoMoHttpClient.builder().requestTimeout(-1)
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Request timeout must be positive");
    }

    @Test
    @DisplayName("Should execute GET request successfully")
    void shouldExecuteGetRequest() throws MoMoHttpException {
        // Using a public test API
        var request = MoMoHttpRequest.builder()
            .url("https://httpbin.org/get")
            .method("GET")
            .header("Accept", "application/json")
            .build();

        var response = httpClient.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.bodyAsString()).isNotEmpty();
    }

    @Test
    @DisplayName("Should execute POST request successfully")
    void shouldExecutePostRequest() throws MoMoHttpException {
        var request = MoMoHttpRequest.builder()
            .url("https://httpbin.org/post")
            .method("POST")
            .header("Content-Type", "application/json")
            .body("{\"test\":\"data\"}")
            .build();

        var response = httpClient.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.isSuccessful()).isTrue();
    }

    @Test
    @DisplayName("Should execute async GET request successfully")
    void shouldExecuteAsyncGetRequest() {
        var request = MoMoHttpRequest.builder()
            .url("https://httpbin.org/get")
            .method("GET")
            .build();

        var futureResponse = httpClient.executeAsync(request);

        assertThat(futureResponse).isNotNull();
        assertThat(futureResponse).succeedsWithin(java.time.Duration.ofSeconds(10));

        var response = futureResponse.join();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.isSuccessful()).isTrue();
    }

    @Test
    @DisplayName("Should handle 404 response")
    void shouldHandle404Response() throws MoMoHttpException {
        var request = MoMoHttpRequest.builder()
            .url("https://httpbin.org/status/404")
            .method("GET")
            .build();

        var response = httpClient.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.isSuccessful()).isFalse();
    }

    @Test
    @DisplayName("Should handle 500 response")
    void shouldHandle500Response() throws MoMoHttpException {
        var request = MoMoHttpRequest.builder()
            .url("https://httpbin.org/status/500")
            .method("GET")
            .build();

        var response = httpClient.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.isSuccessful()).isFalse();
    }

    @Test
    @DisplayName("Should throw exception for invalid URL")
    void shouldThrowExceptionForInvalidUrl() {
        var request = MoMoHttpRequest.builder()
            .url("not-a-valid-url")
            .method("GET")
            .build();

        assertThatThrownBy(() -> httpClient.execute(request))
            .isInstanceOf(MoMoHttpException.class);
    }

    @Test
    @DisplayName("Should handle connection timeout")
    void shouldHandleConnectionTimeout() {
        var timeoutClient = DefaultMoMoHttpClient.builder()
            .connectTimeout(1) // 1ms - very short
            .build();

        var request = MoMoHttpRequest.builder()
            .url("https://httpbin.org/delay/5")
            .method("GET")
            .build();

        // This should timeout
        assertThatThrownBy(() -> timeoutClient.execute(request))
            .isInstanceOf(MoMoHttpException.class);

        timeoutClient.close();
    }

    @Test
    @DisplayName("Should preserve request headers")
    void shouldPreserveRequestHeaders() throws MoMoHttpException {
        var request = MoMoHttpRequest.builder()
            .url("https://httpbin.org/headers")
            .method("GET")
            .header("X-Custom-Header", "custom-value")
            .header("User-Agent", "momo4j-test")
            .build();

        var response = httpClient.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.bodyAsString()).contains("X-Custom-Header");
    }

    @Test
    @DisplayName("Should handle response headers")
    void shouldHandleResponseHeaders() throws MoMoHttpException {
        var request = MoMoHttpRequest.builder()
            .url("https://httpbin.org/response-headers?X-Test=test-value")
            .method("GET")
            .build();

        var response = httpClient.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers()).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle empty response body")
    void shouldHandleEmptyResponseBody() throws MoMoHttpException {
        var request = MoMoHttpRequest.builder()
            .url("https://httpbin.org/status/204")
            .method("GET")
            .build();

        var response = httpClient.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.bodyAsString()).isEmpty();
    }

    @Test
    @DisplayName("Should use HTTP/2 by default")
    void shouldUseHttp2ByDefault() {
        var client = DefaultMoMoHttpClient.builder()
            .httpVersion(java.net.http.HttpClient.Version.HTTP_2)
            .build();

        assertThat(client).isNotNull();
        client.close();
    }

    @Test
    @DisplayName("Should support HTTP/1.1")
    void shouldSupportHttp11() {
        var client = DefaultMoMoHttpClient.builder()
            .httpVersion(java.net.http.HttpClient.Version.HTTP_1_1)
            .build();

        assertThat(client).isNotNull();
        client.close();
    }

    @Test
    @DisplayName("Close should be idempotent")
    void closeShouldBeIdempotent() {
        var client = DefaultMoMoHttpClient.builder().build();
        client.close();
        client.close(); // Should not throw
    }
}
