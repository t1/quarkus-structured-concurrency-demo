package com.example;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.BDDAssertions.then;

@QuarkusTest
@SuppressWarnings("preview")
public class ExampleGraphQLTest {
    @GraphQLClientApi(endpoint = "http://localhost:8080/graphql")
    interface Api {
        String hello(Integer context);

        String part(int n);
    }

    @Inject Api api;

    @Test
    @Timeout(value = 10)
    public void testHelloPart() {
        var hello = api.part(1);

        then(hello).isEqualTo("Hello");
    }

    @Test
    @Timeout(value = 10)
    public void testWorldPart() {
        var world = api.part(2);

        then(world).isEqualTo("World");
    }

    //    @Test
    @RepeatedTest(value = 10)
    // this test gets slower when testManyHellos already brings the service to the limits
    // @Timeout(value = 1_999, unit = MILLISECONDS) // must be faster than two seconds
    @Timeout(value = 10)
    public void testHelloEndpoint() {
        var hello = api.hello(-1);

        then(hello).isEqualTo("Hello, World");
    }

    @Disabled @Test
    public void testManyHellos() throws Exception {
        var n = 10;
        var counter = new AtomicInteger();
        try (var scope = new java.util.concurrent.StructuredTaskScope.ShutdownOnFailure()) {
            var tasks = IntStream.range(0, n)
                    .mapToObj(i -> scope.fork(() -> api.hello(i)))
                    .toList();

            scope.joinUntil(Instant.now().plusSeconds(30)).throwIfFailed();

            tasks.stream().map(java.util.concurrent.StructuredTaskScope.Subtask::get).forEach(text -> {
                counter.incrementAndGet();
                then(text).isEqualTo("Hello, World");
            });
        }

        then(counter.get()).isEqualTo(n);
    }
}
