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

public class TestStores {

	int storeID = 0;
	String storeName = "";

	@BeforeTest
	public void init() {
		RestAssured.baseURI = "http://localhost:3030";
		RestAssured.port = 3030;
	}

	@Test
	public void getAllStores() {
		int productListSize = RestAssured.given().params("$limit", 6, "$skip", 3).get("/stores").then().assertThat()
				.statusCode(200).and().body("limit", equalTo(6)).extract().body().jsonPath().getList("data").size();
		Assert.assertEquals(productListSize, 6);
	}

	@Test
	public void addStoreData() {
		JsonPath jsonPath = new JsonPath(new File("./src/test/resources/inputJSONFiles/requestAddStores.json"));
		String reqStoreName = jsonPath.get("name");

		// store id in variable Response response =
		Response response = RestAssured.given().contentType(ContentType.JSON)
				.body(new File("./src/test/resources/inputJSONFiles/requestAddStores.json")).post("/stores").then()
				.assertThat().statusCode(201).and().body("name", equalTo(reqStoreName)).extract().response();
		JsonPath js = response.jsonPath();
		// To pass in next delete request , need ID
		storeID = js.getInt("id"); // For validation in delete request ,need "name" parameter value
		storeName = js.getString("name");
	}

	@Test(dependsOnMethods = "addStoreData")
	public void getByStoreID() {
		RestAssured.given().param("id", storeID).get("/stores ").then().assertThat().body("data[0].name",
				equalTo(storeName));

	}

	@Test(dependsOnMethods = "getByStoreID")
	public void updateStorebyID() {

		Response response = RestAssured.given().contentType(ContentType.JSON).pathParam("id", storeID)
				.body(new File("./src/test/resources/inputJSONFiles/requestUpdateStores.json")).patch("/stores/{id}")
				.then().assertThat().statusCode(200).extract().response();

		JsonPath js = response.jsonPath();
		storeName = js.getString("name");
		// For validation in delete request ,need "name" parameter value
		Assert.assertEquals(js.getString("name"), storeName);

	}

	@Test(dependsOnMethods = "updateStorebyID")
	public void deleteStoreData() {

		Response response = RestAssured.given().contentType(ContentType.JSON).pathParam("id", storeID)
				.delete("/stores/{id}").then().assertThat().extract().response();
		JsonPath js = response.jsonPath();
		Assert.assertEquals(js.getString("name"), storeName);
	}

	@Test // Test Invalid ID as input for Get Store request
	public void invalidStoreID() {
		String errorMsg = "No record found for id '-123'";

		Response res = RestAssured.given().pathParam("id", "-123").get("/stores/{id}").then().assertThat()
				.statusCode(404).extract().response();

		JsonPath jsonPath = res.jsonPath();
		Assert.assertEquals(jsonPath.getString("message"), errorMsg);
	}

	@Test(dependsOnMethods = "deleteStoreData")
	// Negative test case:Verify error response by querying deleted Store ID
	public void getByDeletedStoreID() {
		String errorMsg1 = "NotFound";
		String errorMsg2 = "not-found";
		Response response = RestAssured.given().pathParam("id", storeID).get("/stores/{id}").then().assertThat()
				.statusCode(404).extract().response();
		JsonPath js = response.jsonPath();
		Assert.assertEquals(js.getString("name"), errorMsg1);
		Assert.assertEquals(js.getString("className"), errorMsg2);

	}

}
