package com.example;

import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@GraphQLApi
//@RunOnVirtualThread
public class ExampleGraphQL {
    private static final Logger log = LoggerFactory.getLogger(ExampleGraphQL.class);
    private final AtomicInteger parallel = new AtomicInteger();

    @GraphQLClientApi(endpoint = "http://localhost:8080/graphql")
    interface Api {
        String part(int n, Integer context);
    }

    @Inject Api api;

    @Query
    public String hello(Integer context) {
        log.info("start hello {} [#{}]", context, parallel.incrementAndGet());
        try (var scope = scope()) {
            var hello = scope.fork(() -> api.part(1, context));
            var world = scope.fork(() -> api.part(2, context));

            scope.joinUntil(Instant.now().plusSeconds(10)).throwIfFailed();

            return hello.get() + ", " + world.get();
        } finally {
            log.info("end hello {} [#{}]", context, parallel.getAndDecrement());
        }
    }

    private static MyScope scope() {
        return new MyScope(); //new StructuredTaskScope.ShutdownOnFailure();
    }

    private static class MyScope implements AutoCloseable {
        @Override public void close() {}

        public <T> Supplier<T> fork(Supplier<T> supplier) {
            return supplier;
        }
        // public <T> CompletableFuture<T> fork(Supplier<T> supplier) {
        //     return CompletableFuture.supplyAsync(supplier);
        // }

        public MyScope joinUntil(Instant instant) {
            return this;
        }

        public void throwIfFailed() {
        }
    }

    @Query
    public String part(int n, Integer context) throws InterruptedException {
        log.info("start part {} for {} [#{}]", n, context, parallel.incrementAndGet());
        Thread.sleep(100);
        log.info("done sleeping for part {} for {} [#{}]", n, context, parallel.getAndDecrement());
        return switch (n) {
            case 1 -> "Hello";
            case 2 -> "World";
            default -> throw new RuntimeException("invalid part number " + n);
        };
    }
}
