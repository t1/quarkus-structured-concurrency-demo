package com.example;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.TimeoutException;

import static org.assertj.core.api.BDDAssertions.fail;

class TimeoutTest {
    private static final Logger log = LoggerFactory.getLogger(TimeoutTest.class);

    @Test void shouldTimeOut() throws InterruptedException {
        try (var scope = StructuredTaskScope.open(
                Joiner.allSuccessfulOrThrow(),
                config -> config.withTimeout(Duration.ofMillis(10)))) {
            scope.fork(() -> log.info("one"));
            scope.fork(() -> log.info("two"));
            scope.fork(() -> {
                Thread.sleep(100);
                return 1; // make this lambda a Callable (not a Runnable), so it can throw an exception
            });

            scope.join();

            fail("this should not be reached");
        } catch (TimeoutException e) {
            log.info("this is fine");
        }
    }

    @Test void shouldUseStackWalker() {
        StackWalker.getInstance().forEach(frame -> log.info("{}", frame));
    }
}
