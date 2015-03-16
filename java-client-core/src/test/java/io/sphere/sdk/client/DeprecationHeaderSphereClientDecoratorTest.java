package io.sphere.sdk.client;


import io.sphere.sdk.http.HttpClient;
import io.sphere.sdk.http.HttpHeaders;
import io.sphere.sdk.http.HttpRequest;
import io.sphere.sdk.http.HttpResponse;
import io.sphere.sdk.models.SphereException;
import org.hamcrest.CustomTypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.fest.assertions.Assertions.assertThat;

public class DeprecationHeaderSphereClientDecoratorTest {
    private static final String DEPRECATION_MESSAGE = "don't use it anymore";
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void execution() throws Exception {
        final HttpHeaders headers = HttpHeaders.of((SphereHttpHeaders.X_DEPRECATION_NOTICE), DEPRECATION_MESSAGE);
        HttpResponse response = HttpResponse.of(200, DummySphereRequest.DEFAULT_RESPONSE_OBJECT, headers);
        final SphereClient sphereClient = clientWithResponse(response);

        assertThat(sphereClient.execute(DummySphereRequest.of()).join())
                .overridingErrorMessage("normal sphere client ignores deprecation header")
                .isEqualTo(DummySphereRequest.DEFAULT_RESPONSE_OBJECT);

        final SphereClient decoratedClient = DeprecationHeaderSphereClientDecorator.of(sphereClient);
        thrown.expect(new CustomTypeSafeMatcher<ExecutionException>("") {
            @Override
            protected boolean matchesSafely(final ExecutionException e) {
                return e.getCause() != null && e.getCause() instanceof SphereException && e.getCause().getMessage().contains("deprecation warning: " + DEPRECATION_MESSAGE);
            }
        });
        decoratedClient.execute(DummySphereRequest.of()).get();
    }

    private SphereClient clientWithResponse(final HttpResponse response) {
        return SphereClient.of(SphereApiConfig.of("test"), new HttpClient() {
            @Override
            public CompletableFuture<HttpResponse> execute(final HttpRequest httpRequest) {
                return CompletableFuture.completedFuture(response);
            }

            @Override
            public void close() {

            }
        }, SphereAccessTokenSupplier.ofConstantToken("foo"));
    }
}