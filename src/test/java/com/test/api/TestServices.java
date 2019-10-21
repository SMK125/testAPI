package com.test.api;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import static org.hamcrest.Matchers.*;

import java.io.File;

public class TestServices {

	String serviceID = "";
	String serviceName = "";

	@BeforeTest
	public void init() {
		RestAssured.baseURI = "http://localhost:3030";
		RestAssured.port = 3030;
	}

	@Test
	public void getAllServiceData() {
		int storeListSize = RestAssured.given().params("$limit", 12, "$skip", 2).get("/services").then().assertThat()
				.statusCode(200).and().body("limit", equalTo(12)).extract().body().jsonPath().getList("data").size();
		Assert.assertEquals(storeListSize, 12);
	}

	@Test
	// This method is to add service data
	public void addServiceData() {
		JsonPath jsonPath = new JsonPath(new File("./src/test/resources/inputJSONFiles/requestAddServices.json"));
		// Store service name from requestFile in " serviceName" and use it for
		// assertion with response serviceName
		serviceName = jsonPath.getString("name");

		Response response = RestAssured.given().contentType(ContentType.JSON)
				.body(new File("./src/test/resources/inputJSONFiles/requestAddServices.json")).post("/services").then()
				.assertThat().statusCode(201).and().body("name", equalTo(serviceName)).extract().response();
		JsonPath js = response.jsonPath();
		// To pass in next delete request , need ID
		serviceID = js.getString("id");
		// For validation in delete request ,need "name" parameter value
		serviceName = js.getString("name");

	}

	@Test(dependsOnMethods = "addServiceData")
	// This method is to get Service data by serviceID
	public void getByServiceID() {
		Response response = RestAssured.given().pathParam("id", serviceID).get("/services/{id}").andReturn();
		JsonPath js = response.jsonPath();
		Assert.assertEquals(js.getString("id"), serviceID);
		Assert.assertEquals(js.getString("name"), serviceName);
	}

	@Test(dependsOnMethods = "getByServiceID")
	public void updateServicebyID() {
		Response response = RestAssured.given().contentType(ContentType.JSON).pathParam("id", serviceID)
				.body(new File("./src/test/resources/inputJSONFiles/requestUpdateServices.json"))
				.patch("/services/{id}").then().assertThat().statusCode(200).extract().response();

		JsonPath js = response.jsonPath();
		serviceName = js.getString("name");
		serviceID = js.getString("id");
		// Validations
		Assert.assertEquals(js.getString("name"), serviceName);
		Assert.assertEquals(js.getString("id"), serviceID);

	}

	@Test(dependsOnMethods = "updateServicebyID")
	public void deleteServicebyID() {
		Response response = RestAssured.given().contentType(ContentType.JSON).pathParam("id", serviceID)
				.delete("/services/{id}").then().assertThat().statusCode(200).extract().response();

		JsonPath js = response.jsonPath();
		Assert.assertEquals(js.getString("name"), serviceName);
		Assert.assertEquals(js.getString("id"), serviceID);
	}

	// Negative test case:Verify error response by querying deleted service ID
	@Test(dependsOnMethods = "deleteServicebyID")
	public void getByDeletedServiceID() {
		String errorMsg1 = "NotFound";
		String errorMsg2 = "not-found";
		Response response = RestAssured.given().pathParam("id", serviceID).get("/services/{id}").then().assertThat()
				.statusCode(404).extract().response();
		JsonPath js = response.jsonPath();
		Assert.assertEquals(js.getString("name"), errorMsg1);
		Assert.assertEquals(js.getString("className"), errorMsg2);
	}

}
