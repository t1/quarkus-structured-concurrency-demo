package com.example;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.assertj.core.api.BDDAssertions.then;

@QuarkusTest
public class ExampleResourceTest {
    @RegisterRestClient(baseUri = "http://localhost:8080")
    interface Api {
        @GET @Path("/hello")
        @Produces(TEXT_PLAIN)
        String hello(@QueryParam("n") Integer n);

        @GET @Path("/part/{n}")
        @Produces(TEXT_PLAIN)
        String part(@PathParam("n") int n);
    }

    @Inject @RestClient Api api;

    @Test
    public void testHelloPart() {
        var hello = api.part(1);

        then(hello).isEqualTo("Hello");
    }

    @Test
    public void testWorldPart() {
        var world = api.part(2);

        then(world).isEqualTo("World");
    }

    @Test
    // this test gets slower when testManyHellos already brings the service to the limits
    // @Timeout(value = 1_999, unit = MILLISECONDS) // must be faster than two seconds
    public void testHelloEndpoint() {
        var hello = api.hello(-1);

        then(hello).isEqualTo("Hello, World");
    }

    @Test
    public void testManyHellos() throws Exception {
        var n = 100;
        var counter = new AtomicInteger();
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var tasks = IntStream.range(0, n)
                    .mapToObj(i -> scope.fork(() -> api.hello(i)))
                    .toList();

            scope.joinUntil(Instant.now().plusSeconds(30)).throwIfFailed();

            tasks.stream().map(StructuredTaskScope.Subtask::get).forEach(text -> {
                counter.incrementAndGet();
                then(text).isEqualTo("Hello, World");
            });
        }

        then(counter.get()).isEqualTo(n);
    }

    @Test
    public void testManySleeps() throws Exception {
        var n = 1_000_000;
        var counter = new AtomicInteger();
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var tasks = IntStream.range(0, n)
                    .mapToObj(i -> scope.fork(() -> afterDelayReturn(i)))
                    .toList();

            scope.joinUntil(Instant.now().plusSeconds(30)).throwIfFailed();

            tasks.stream().map(StructuredTaskScope.Subtask::get).forEach(ignored -> counter.incrementAndGet());
        }

        then(counter.get()).isEqualTo(n);
    }

    private static int afterDelayReturn(int i) {
        try {
            Thread.sleep(1000);
            return i;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
