package org.openlmis.order.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openlmis.order.domain.Order;
import org.openlmis.order.domain.OrderLineItem;
import org.openlmis.order.domain.OrderStatus;
import org.openlmis.order.domain.Requisition;
import org.openlmis.order.domain.RequisitionStatus;
import org.openlmis.order.referencedata.model.FacilityDto;
import org.openlmis.order.referencedata.model.OrderableProductDto;
import org.openlmis.order.referencedata.model.ProcessingPeriodDto;
import org.openlmis.order.referencedata.model.ProcessingScheduleDto;
import org.openlmis.order.referencedata.model.ProgramDto;
import org.openlmis.order.referencedata.model.SupervisoryNodeDto;
import org.openlmis.order.repository.OrderLineItemRepository;
import org.openlmis.order.repository.OrderRepository;
import org.openlmis.order.repository.RequisitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import guru.nidi.ramltester.junit.RamlMatchers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

@Ignore
public class OrderLineItemControllerIntegrationTest extends BaseWebIntegrationTest {

  private static final String RESOURCE_URL = "/api/orderLineItems";
  private static final String ID_URL = RESOURCE_URL + "/{id}";
  private static final String ACCESS_TOKEN = "access_token";
  private static final UUID ID = UUID.fromString("1752b457-0a4b-4de0-bf94-5a6a8002427e");

  @Autowired
  private OrderLineItemRepository orderLineItemRepository;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private RequisitionRepository requisitionRepository;

  private OrderLineItem orderLineItem = new OrderLineItem();
  private Order order = new Order();

  @Before
  public void setUp() {

    OrderableProductDto product = new OrderableProductDto();
    product.setId(UUID.randomUUID());

    FacilityDto facility = new FacilityDto();
    facility.setId(UUID.randomUUID());
    facility.setCode("facilityCode");
    facility.setName("facilityName");
    facility.setDescription("facilityDescription");
    facility.setActive(true);
    facility.setEnabled(true);

    SupervisoryNodeDto supervisoryNode = new SupervisoryNodeDto();
    supervisoryNode.setId(UUID.randomUUID());
    supervisoryNode.setCode("NodeCode");
    supervisoryNode.setName("NodeName");
    supervisoryNode.setFacility(facility);

    ProgramDto program = new ProgramDto();
    program.setId(UUID.randomUUID());
    program.setCode("programCode");

    ProcessingScheduleDto schedule = new ProcessingScheduleDto();
    schedule.setCode("scheduleCode");
    schedule.setName("scheduleName");

    ProcessingPeriodDto period = new ProcessingPeriodDto();
    period.setId(UUID.randomUUID());
    period.setProcessingSchedule(new ProcessingScheduleDto());
    period.setName("periodName");
    period.setStartDate(LocalDate.of(2015, Month.JANUARY, 1));
    period.setEndDate(LocalDate.of(2015, Month.DECEMBER, 31));

    Requisition requisition = new Requisition();
    requisition.setProgramId(program.getId());
    requisition.setFacilityId(facility.getId());
    requisition.setProcessingPeriodId(period.getId());
    requisition.setStatus(RequisitionStatus.INITIATED);
    requisition.setEmergency(false);
    requisition.setSupervisoryNodeId(supervisoryNode.getId());
    requisitionRepository.save(requisition);

    orderLineItem.setOrder(order);
    orderLineItem.setOrderableProductId(product.getId());
    orderLineItem.setOrderedQuantity(100L);
    orderLineItem.setFilledQuantity(100L);

    order.setRequisition(requisition);
    order.setOrderCode("O");
    order.setQuotedCost(new BigDecimal("10.00"));
    order.setStatus(OrderStatus.ORDERED);
    order.setProgramId(program.getId());
    order.setCreatedById(UUID.randomUUID());
    order.setRequestingFacilityId(facility.getId());
    order.setReceivingFacilityId(facility.getId());
    order.setSupplyingFacilityId(facility.getId());
    order.setOrderLineItems(new ArrayList<>());
    order.getOrderLineItems().add(orderLineItem);
    orderRepository.save(order);
  }

  @Test
  public void shouldCreateOrder() {

    orderLineItemRepository.delete(orderLineItem);

    restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(orderLineItem)
        .when()
        .post(RESOURCE_URL)
        .then()
        .statusCode(201);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldGetAllOrders() {

    OrderLineItem[] response = restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .when()
        .get(RESOURCE_URL)
        .then()
        .statusCode(200)
        .extract().as(OrderLineItem[].class);

    Iterable<OrderLineItem> orderLineItems = Arrays.asList(response);
    assertTrue(orderLineItems.iterator().hasNext());
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldUpdateOrderLineItem() {

    orderLineItem.setOrderedQuantity(100L);

    OrderLineItem response = restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .pathParam("id", orderLineItem.getId())
        .body(orderLineItem)
        .when()
        .put(ID_URL)
        .then()
        .statusCode(200)
        .extract().as(OrderLineItem.class);

    assertTrue(response.getOrderedQuantity().equals(100L));
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldCreateNewOrderLineItemIfDoesNotExist() {

    orderLineItemRepository.delete(orderLineItem);
    orderLineItem.setOrderedQuantity(100L);

    OrderLineItem response = restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .pathParam("id", ID)
        .body(orderLineItem)
        .when()
        .put(ID_URL)
        .then()
        .statusCode(200)
        .extract().as(OrderLineItem.class);

    assertTrue(response.getOrderedQuantity().equals(100L));
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldGetChosenOrderLineItem() {

    OrderLineItem response = restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .pathParam("id", orderLineItem.getId())
        .when()
        .get(ID_URL)
        .then()
        .statusCode(200)
        .extract().as(OrderLineItem.class);

    assertTrue(orderLineItemRepository.exists(response.getId()));
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldNotGetNonexistentOrderLineItem() {

    orderLineItemRepository.delete(orderLineItem);

    restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .pathParam("id", orderLineItem.getId())
        .when()
        .get(ID_URL)
        .then()
        .statusCode(404);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldDeleteOrderLineItem() {

    restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .pathParam("id", orderLineItem.getId())
        .when()
        .delete(ID_URL)
        .then()
        .statusCode(204);

    assertFalse(orderLineItemRepository.exists(orderLineItem.getId()));
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldNotDeleteNonexistentOrderLineItem() {

    orderLineItemRepository.delete(orderLineItem);

    restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .pathParam("id", orderLineItem.getId())
        .when()
        .delete(ID_URL)
        .then()
        .statusCode(404);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }
}
