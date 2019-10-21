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

public class TestProducts {

	int productID = 0;
	String productName = "";

	// This method is to initialize base URI
	@BeforeTest	
	public void init() {
		RestAssured.baseURI = "http://localhost:3030";
		RestAssured.port = 3030;
	}

	@Test
	// This method is to get all Products by specifying Limit & Skip
	public void getAllProducts() {
		int productListSize = RestAssured.given().params("$limit", 5, "$skip", 2).get("/products").then().assertThat()
				.statusCode(200).and().body("limit", equalTo(5)).extract().body().jsonPath().getList("data").size();
		Assert.assertEquals(productListSize, 5);
	}

	@Test
	// This method is to get Products with giving only Limit parameter
	public void getProductsWithLimit() {
		int productListSize = RestAssured.given().params("$limit", 1).get("/products").then().assertThat()
				.statusCode(200).and().body("limit", equalTo(1), "skip", equalTo(0)).extract().body().jsonPath()
				.getList("data").size();
		Assert.assertEquals(productListSize, 1);
	}

	@Test
	// This method is to get Products with giving only Skip parameter
	public void getProductsWithSkip() {
		int productListSize = RestAssured.given().params("$skip", 10).get("/products").then().assertThat()
				.statusCode(200).and().body("limit", equalTo(10), "skip", equalTo(10)).extract().body().jsonPath()
				.getList("data").size();
		Assert.assertEquals(productListSize, 10);
	}

	@Test
	// This method is to Add Products with all mandatory parameters
	public void addProduct() {
		JsonPath jsonPath = new JsonPath(new File("./src/test/resources/inputJSONFiles/requestAddProduct.json"));
		String reqProdName = jsonPath.get("name");

		Response response = RestAssured.given().contentType(ContentType.JSON)
				.body(new File("./src/test/resources/inputJSONFiles/requestAddProduct.json")).post("/products").then()
				.assertThat().statusCode(201).and().body("name", equalTo(reqProdName)).extract().response();
		JsonPath js = response.jsonPath();
		// To pass in next getByProductID request , need ID
		productID = js.getInt("id");
		// To pass in next getByProductID
		productName = js.getString("name");
	}

	@Test(dependsOnMethods = "addProduct")
	// This method is to Get products by ProductID added in "addProduct" method
	public void getByProductID() {
		RestAssured.given().param("id", productID).get("/products").then().assertThat().body("data[0].name",
				equalTo(productName));

	}

	@Test(dependsOnMethods = "getByProductID")
	// This method is to Update product data with ProductID parameter fetched from
	// "getByProductID" method
	public void updateProductbyID() {

		Response response = RestAssured.given().contentType(ContentType.JSON).pathParam("id", productID)
				.body(new File("./src/test/resources/inputJSONFiles/requestUpdateProduct.json")).patch("/products/{id}")
				.then().assertThat().statusCode(200).extract().response();

		JsonPath js = response.jsonPath();
		productName = js.getString("name");
		// For validation in delete request ,need "name" parameter value
		Assert.assertEquals(js.getString("name"), productName);

	}

	@Test(dependsOnMethods = "updateProductbyID")
	// This method is to delete product by ID
	public void deleteProductbyID() {
		Response response = RestAssured.given().contentType(ContentType.JSON).pathParam("id", productID)
				.delete("/products/{id}").then().assertThat().extract().response();

		JsonPath js = response.jsonPath();
		Assert.assertEquals(js.getString("name"), productName);
	}

	@Test
	// Test Invalid ID as input for getProduct request
	public void invalidProductID() {
		String errorMsg1 = "No record found for id '123'";
		String errorMsg2 = "NotFound";
		Response res = RestAssured.given().pathParam("id", 123).get("/products/{id}").then().assertThat()
				.statusCode(404).and().contentType(ContentType.JSON).extract().response();

		JsonPath jsonPath = res.jsonPath();

		Assert.assertEquals(jsonPath.getString("message"), errorMsg1);
		Assert.assertEquals(jsonPath.getString("name"), errorMsg2);
		Assert.assertEquals(jsonPath.getString("className"), "not-found");

	}

	@Test
	// Test mandatory parameters in Post request
	public void invalidPostProdBody() {

		String errorMsg = "should have required property 'name'";
		Response response = RestAssured.given().contentType(ContentType.JSON)
				.body(new File("./src/test/resources/inputJSONFiles/requestInvalidAddProduct.json")).post("/products")
				.then().assertThat().statusCode(400).extract().response();
		JsonPath jsonPath = response.jsonPath();
		Assert.assertEquals(jsonPath.get("errors[0]"), errorMsg);

	}

	@Test(dependsOnMethods = "deleteProductbyID")
	// Negative test case:Verify error response by querying deleted product ID
	public void getByDeletedProductID() {
		String errorMsg1 = "NotFound";
		String errorMsg2 = "not-found";
		Response response = RestAssured.given().pathParam("id", productID).get("/products/{id}").then().assertThat()
				.statusCode(404).extract().response();
		JsonPath js = response.jsonPath();
		Assert.assertEquals(js.getString("name"), errorMsg1);
		Assert.assertEquals(js.getString("className"), errorMsg2);
	}

}
