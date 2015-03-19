package be.g00glen00b.controller;

import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

import org.apache.http.HttpStatus;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import be.g00glen00b.Application;
import be.g00glen00b.builders.ItemBuilder;
import be.g00glen00b.model.Item;
import be.g00glen00b.repository.ItemRepository;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest("server.port:0")
public class ItemControllerIT {   
  private static final String CHECKED_FIELD = "checked";
  private static final String DESCRIPTION_FIELD = "description";
  private static final String ITEMS_RESOURCE = "/items";
  private static final String ITEM_RESOURCE = "/items/{id}";
  private static final int NON_EXISTING_ID = 999;
  private static final String FIRST_ITEM_DESCRIPTION = "First item";
  private static final String SECOND_ITEM_DESCRIPTION = "Second item";
  private static final String THIRD_ITEM_DESCRIPTION = "Third item";
  private static final Item FIRST_ITEM = new ItemBuilder()
    .description(FIRST_ITEM_DESCRIPTION)
    .checked()
    .build();
  private static final Item SECOND_ITEM = new ItemBuilder()
    .description(SECOND_ITEM_DESCRIPTION)
    .build();
  private static final Item THIRD_ITEM = new ItemBuilder()
    .description(THIRD_ITEM_DESCRIPTION)
    .build();
  @Autowired
  private ItemRepository repository;
  @Value("${local.server.port}")
  private int serverPort;
  private Item firstItem;
  private Item secondItem;
  private String url;
  
  @Before
  public void setUp() {
    repository.deleteAll();
    firstItem = repository.save(FIRST_ITEM);
    secondItem = repository.save(SECOND_ITEM);
    RestAssured.port = serverPort;
    url = "http://localhost:" + port + "/items";
  }

  @Test
  public void getItemsShouldReturnBothItems() {
    when()
      .get(ITEMS_RESOURCE)
    .then().log().all()
      .statusCode(HttpStatus.SC_OK)
      .body("_embedded.items.description", hasItems(FIRST_ITEM_DESCRIPTION, SECOND_ITEM_DESCRIPTION))
      .body("_embedded.items.checked", hasItems(true, false))
      .body("_embedded.items._links.self.href", 
		hasItems(url + "/" + FIRST_ITEM.getId(), 
			url + "/" + SECOND_ITEM.getId()))
	.body("_links.self.templated", is(true))
	.body("_links.self.href", is(url + "{?page,size,sort}"))
	
	.body("page.size", is(20)).body("page.totalElements", is(2))
	.body("page.totalPages", is(1)).body("page.number", is(0));
  }
  
  @Test
  public void addItemShouldCreate() {
    given()
      .body(THIRD_ITEM)
      .contentType(ContentType.JSON)
    .when()
      .post(ITEMS_RESOURCE)
    .then().log().all()
      .statusCode(HttpStatus.SC_CREATED);
  }
  
  @Test
  public void addItemShouldReturnBadRequestWithoutBody() {
    when()
      .post(ITEMS_RESOURCE)
    .then().log().all()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }
  
  @Test
  public void addItemShouldReturnBadRequestIfNonJSON() {
    given()
      .body(THIRD_ITEM)
    .when()
      .post(ITEMS_RESOURCE)
    .then().log().all()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }
  
  @Test
  public void updateItemShouldReturnNoContent() {
    // Given an unchecked first item
    Item item = new ItemBuilder()
      .description(FIRST_ITEM_DESCRIPTION)
      .build();
    given()
      .body(item)
      .contentType(ContentType.JSON)
    .when()
      .put(ITEM_RESOURCE, firstItem.getId())
    .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);
  }
  
  @Test
  public void updateItemShouldReturnBadRequestWithoutBody() {
    when()
      .put(ITEM_RESOURCE, firstItem.getId())
    .then().log().all()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }
  
  @Test
  public void updateItemShouldReturnBadRequestIfNonJSON() {
    given()
      .body(FIRST_ITEM)
    .when()
      .put(ITEM_RESOURCE, firstItem.getId())
    .then().log().all()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }
  
  @Test
  public void updateAnyNonExistingItemShouldCreate() {
    assertThat(THIRD_ITEM.getId(), is(nullValue()));
      
    given()
      .body(THIRD_ITEM)
      .contentType(ContentType.JSON)
    .when()
      .put(ITEM_RESOURCE, NON_EXISTING_ID)
    .then().log().all()
      .statusCode(HttpStatus.SC_CREATED);
  }
  
  @Test
  public void deleteItemShouldReturnNoContent() {
    when()
      .delete(ITEM_RESOURCE, secondItem.getId())
    .then().log().all()
      .statusCode(HttpStatus.SC_NO_CONTENT);
  }
  
  @Test
  public void deleteItemShouldBeNotFoundIfNonExistingID() {
    when()
      .delete(ITEM_RESOURCE, NON_EXISTING_ID)
    .then().log().all()
      .statusCode(HttpStatus.SC_NOT_FOUND);
  }
}
