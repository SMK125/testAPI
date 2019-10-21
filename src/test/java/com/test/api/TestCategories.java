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

public class TestCategories {

	String categoryID = "";
	String categoryName = "";

	@BeforeTest
	public void init() {
		RestAssured.baseURI = "http://localhost:3030";
		RestAssured.port = 3030;
	}

	@Test
	public void getAllCategories() {
		int categoryListSize = RestAssured.given().params("$limit", 10, "$skip", 2).get("/categories").then()
				.assertThat().statusCode(200).and().body("limit", equalTo(10)).extract().body().jsonPath()
				.getList("data").size();
		Assert.assertEquals(categoryListSize, 10);
	}

	@Test
	public void addCategory() {
		JsonPath jsonPath = new JsonPath(new File("./src/test/resources/inputJSONFiles/requestAddCategory.json"));
		categoryName = jsonPath.getString("name");
		categoryID = jsonPath.getString("id");

		Response response = RestAssured.given().contentType(ContentType.JSON)
				.body(new File("./src/test/resources/inputJSONFiles/requestAddCategory.json")).post("/categories")
				.then().assertThat().statusCode(201).and()
				.body("name", equalTo(categoryName), "id", equalTo(categoryID)).extract().response();
		JsonPath js = response.jsonPath();
		// To pass in next delete request , need ID
		categoryID = js.getString("id");
		// For validation in delete request ,need "name" parameter value
		categoryName = js.getString("name");
	}

	@Test(dependsOnMethods = "addCategory")

	public void getByCategoryID() {

		RestAssured.given().pathParam("id", categoryID).get("/categories/{id}").then().assertThat().body("name",
				equalTo(categoryName), "id", equalTo(categoryID));

	}

	@Test(dependsOnMethods = "getByCategoryID")

	public void updateCategoryID() {

		Response response = RestAssured.given().contentType(ContentType.JSON).pathParam("id", categoryID)
				.body(new File("./src/test/resources/inputJSONFiles/requestUpdateCategory.json"))
				.patch("/categories/{id}").then().assertThat().statusCode(200).extract().response();

		JsonPath js = response.jsonPath();
		categoryName = js.getString("name");
		categoryID = js.getString("id");

		// For validation in delete request ,need "name" parameter value
		Assert.assertEquals(js.getString("name"), categoryName);
		Assert.assertEquals(js.getString("id"), categoryID);

	}

	@Test(dependsOnMethods = "updateCategoryID")
	public void deleteCategorybyID() {
		Response response = RestAssured.given().contentType(ContentType.JSON).pathParam("id", categoryID)
				.delete("/categories/{id}").then().assertThat().statusCode(200).extract().response();
		JsonPath js = response.jsonPath();
		// Assertions
		Assert.assertEquals(js.getString("name"), categoryName);
		Assert.assertEquals(js.getString("id"), categoryID);
	}

	@Test // Test Invalid ID as input for getCategory request
	public void invalidCategoryID() {
		String errorMsg = "No record found for id 'abc123'";

		Response res = RestAssured.given().pathParam("id", "abc123").get("/categories/{id}").then().assertThat()
				.statusCode(404).extract().response();

		JsonPath jsonPath = res.jsonPath();
		Assert.assertEquals(jsonPath.getString("message"), errorMsg);
	}

	@Test
	// Test mandatory parameter-Category Name in Post Request
	public void missingCategoryNamePost() {

		String errorMsg = "should have required property 'name'";
		Response response = RestAssured.given().contentType(ContentType.JSON)
				.body(new File("./src/test/resources/inputJSONFiles/requestMissingCatName.json")).post("/categories")
				.then().assertThat().statusCode(400).extract().response();
		JsonPath jsonPath = response.jsonPath();
		Assert.assertEquals(jsonPath.get("errors[0]"), errorMsg);

	}

	@Test
	// Test invalid ID in Delete request
	public void invalidCategoryIDDelete() {

		String errorMsg = "No record found for id 'xyz123'";
		Response response = RestAssured.given().contentType(ContentType.JSON).pathParam("id", "xyz123")
				.delete("/categories/{id}").then().assertThat().statusCode(404).extract().response();
		JsonPath js = response.jsonPath();
		Assert.assertEquals(js.get("message"), errorMsg);
	}

	@Test(dependsOnMethods = "deleteCategorybyID")
	// Negative test case:Verify error response by querying deleted product ID
	public void getByDeletedCategoryID() {
		String errorMsg1 = "NotFound";
		String errorMsg2 = "not-found";
		Response response = RestAssured.given().pathParam("id", categoryID).get("/categories/{id}").then().assertThat()
				.statusCode(404).extract().response();
		JsonPath js = response.jsonPath();
		Assert.assertEquals(js.getString("name"), errorMsg1);
		Assert.assertEquals(js.getString("className"), errorMsg2);
	}

}
