package com.example;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;

@Path("/")
public class ExampleResource {

    @RegisterRestClient(baseUri = "http://localhost:8080")
    interface Api {
        @GET @Path("/part/{n}")
        @Produces(MediaType.TEXT_PLAIN)
        String part(@PathParam("n") int n);
    }

    @Inject @RestClient Api api;

    @GET @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    @RunOnVirtualThread
    public String structuredConcurrencyExample() throws InterruptedException, ExecutionException {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var task1 = scope.fork(() -> api.part(1));
            var task2 = scope.fork(() -> api.part(2));

            scope.join().throwIfFailed();

            return task1.get() + ", " + task2.get();
        }
    }

    @GET @Path("/part/{n}")
    @Produces(MediaType.TEXT_PLAIN)
    @RunOnVirtualThread
    public String part(@PathParam("n") int n) {
        return switch (n) {
            case 1 -> "Hello";
            case 2 -> "World";
            default -> throw new BadRequestException("invalid part number " + n);
        };
    }
}
