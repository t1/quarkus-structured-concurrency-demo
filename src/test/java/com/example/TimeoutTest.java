package com.example;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.TimeoutException;

import static org.assertj.core.api.BDDAssertions.fail;

class TimeoutTest {
    @Test
    void shouldTimeOut() throws InterruptedException {
        try (var scope = StructuredTaskScope.open(
                Joiner.allSuccessfulOrThrow(),
                config -> config.withTimeout(Duration.ofMillis(10)))) {
            scope.fork(() -> System.out.println("one"));
            scope.fork(() -> System.out.println("two"));
            scope.fork(() -> {
                Thread.sleep(100);
                return 1; // make this lambda a Callable (not a Runnable), so it can throw an exception
            });

            scope.join();

            fail("this should not be reached");
        } catch (TimeoutException e) {
            System.out.println("this is fine");
        }
    }
}
