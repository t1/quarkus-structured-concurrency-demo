package com.example;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class ExampleResourceTest {

    @Test
    public void testHelloPart() {
        given()
                .when().get("/part/1")
                .then()
                .statusCode(200)
                .body(is("Hello"));
    }

    @Test
    public void testWorldPart() {
        given()
                .when().get("/part/2")
                .then()
                .statusCode(200)
                .body(is("World"));
    }

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("Hello, World"));
    }

}