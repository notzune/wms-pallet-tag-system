package com.tbg.wms.core.update;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCatalogServiceTest {

    @Test
    void loadCatalog_shouldSeparateStableAndPrereleaseReleases() throws Exception {
        String responseBody = """
                [
                  {
                    "tag_name": "v1.7.2-rc1",
                    "name": "v1.7.2 Release Candidate 1",
                    "html_url": "https://example.test/releases/v1.7.2-rc1",
                    "prerelease": true,
                    "draft": false,
                    "assets": [
                      {
                        "name": "WMS.Pallet.Tag.System-1.7.2-rc1.exe",
                        "browser_download_url": "https://example.test/releases/v1.7.2-rc1/installer.exe"
                      }
                    ]
                  },
                  {
                    "tag_name": "v1.7.1",
                    "name": "v1.7.1",
                    "html_url": "https://example.test/releases/v1.7.1",
                    "prerelease": false,
                    "draft": false,
                    "assets": [
                      {
                        "name": "WMS.Pallet.Tag.System-1.7.1.exe",
                        "browser_download_url": "https://example.test/releases/v1.7.1/installer.exe"
                      }
                    ]
                  }
                ]
                """;

        UpdateCatalogService service = new UpdateCatalogService(
                new FixedResponseHttpClient(200, responseBody),
                new ObjectMapper());

        UpdateCatalogService.ReleaseCatalog catalog = service.loadCatalog("1.7.0");

        assertEquals("1.7.0", catalog.currentVersion());
        assertEquals("1.7.1", catalog.latestStable().version());
        assertEquals("1.7.2-rc1", catalog.latestPrerelease().version());
        assertEquals(List.of("1.7.1"), catalog.stableReleases().stream().map(UpdateCatalogService.ReleaseEntry::version).toList());
        assertEquals(List.of("1.7.2-rc1"), catalog.prereleaseReleases().stream().map(UpdateCatalogService.ReleaseEntry::version).toList());
        assertNotNull(catalog.latestStable().preferredInstallerAsset());
        assertFalse(catalog.latestStable().prerelease());
        assertTrue(catalog.latestPrerelease().prerelease());
    }

    private static final class FixedResponseHttpClient extends HttpClient {
        private final int statusCode;
        private final String body;

        private FixedResponseHttpClient(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return null;
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return HttpClient.Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException, InterruptedException {
            if (request == null || responseBodyHandler == null) {
                throw new IllegalArgumentException("request and responseBodyHandler are required");
            }
            return new FixedResponse<>(statusCode, (T) body, request.uri());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler
        ) {
            return CompletableFuture.completedFuture(new FixedResponse<>(statusCode, (T) body, request.uri()));
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler
        ) {
            return sendAsync(request, responseBodyHandler);
        }
    }

    private record FixedResponse<T>(int statusCode, T body, URI uri) implements HttpResponse<T> {
        @Override
        public HttpRequest request() {
            return HttpRequest.newBuilder(uri).build();
        }

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(java.util.Map.of(), (left, right) -> true);
        }

        @Override
        public T body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return uri;
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
