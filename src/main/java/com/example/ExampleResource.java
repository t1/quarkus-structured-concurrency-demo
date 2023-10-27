package com.example;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicInteger;

@Path("/")
@RunOnVirtualThread
public class ExampleResource {
    private static final Logger log = LoggerFactory.getLogger(ExampleResource.class);
    private final AtomicInteger parallel = new AtomicInteger();

    @RegisterRestClient(baseUri = "http://localhost:8080")
    interface Api {
        @GET @Path("/part/{n}")
        @Produces(MediaType.TEXT_PLAIN)
        String part(@PathParam("n") int n, @QueryParam("n") Integer nn);
    }

    @Inject @RestClient Api api;

    @GET @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@QueryParam("n") Integer n) throws InterruptedException, ExecutionException {
        log.info("start hello {} [#{}]", n, parallel.incrementAndGet());
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var hello = scope.fork(() -> api.part(1, n));
            var world = scope.fork(() -> api.part(2, n));

            scope.join().throwIfFailed();

            return hello.get() + ", " + world.get();
        } finally {
            log.info("end hello {} [#{}]", n, parallel.getAndDecrement());
        }
    }

    @GET @Path("/part/{n}")
    @Produces(MediaType.TEXT_PLAIN)
    public String part(@PathParam("n") int n, @QueryParam("n") Integer nn) throws InterruptedException {
        log.info("start part {} for {} [#{}]", n, nn, parallel.incrementAndGet());
        Thread.sleep(1000);
        log.info("done sleeping for part {} for {} [#{}]", n, nn, parallel.getAndDecrement());
        return switch (n) {
            case 1 -> "Hello";
            case 2 -> "World";
            default -> throw new BadRequestException("invalid part number " + n);
        };
    }
}
