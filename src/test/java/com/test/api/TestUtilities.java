package com.test.api;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import static org.hamcrest.Matchers.*;

public class TestUtilities {

	// Initialize base URI
	@BeforeTest
	public void init() {
		RestAssured.baseURI = "http://localhost:3030";
		RestAssured.port = 3030;
	}

	// Method to get API version
	@Test()
	public void getAPIVersion() {

		RestAssured.given().get("/version").then().assertThat().statusCode(200).and().contentType(ContentType.JSON)
				.body("version", equalTo("1.1.0"));
	}

	// Method to get Healthcheck information about the system
	@Test()
	public void getSystemHealthCheckInfo() {
		// String APIversion;
		RestAssured.given().get("/healthcheck").then().assertThat().statusCode(200).and().and()
				.contentType(ContentType.JSON).body("readonly", equalTo(false));
	}

}
