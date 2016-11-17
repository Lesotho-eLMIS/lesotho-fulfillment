package org.openlmis.order.repository;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openlmis.order.Application;
import org.openlmis.order.domain.Order;
import org.openlmis.order.domain.OrderLineItem;
import org.openlmis.order.domain.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(Application.class)
@Transactional
public class OrderLineItemRepositoryIntegrationTest {

  private static final String ORDER_LINE_ITEM_REPOSITORY_INTEGRATION_TEST
      = "OrderLineItemRepositoryIntegrationTest";

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private OrderLineItemRepository orderLineItemRepository;

  private Order order = new Order();
  private UUID userId = UUID.randomUUID();
  private UUID programId = UUID.randomUUID();
  private UUID requestingFacilityId = UUID.randomUUID();
  private UUID receivingFacilityId = UUID.randomUUID();
  private UUID supplyingFacilityId = UUID.randomUUID();

  @Before
  public void setUp() {
    order.setOrderCode(ORDER_LINE_ITEM_REPOSITORY_INTEGRATION_TEST);
    order.setQuotedCost(new BigDecimal("1.29"));
    order.setStatus(OrderStatus.PICKING);
    order.setProgramId(programId);
    order.setCreatedById(userId);
    order.setRequestingFacilityId(requestingFacilityId);
    order.setReceivingFacilityId(receivingFacilityId);
    order.setSupplyingFacilityId(supplyingFacilityId);
    orderRepository.save(order);
  }

  @Test
  public void testCreate() {
    OrderLineItem orderLineItem = new OrderLineItem();
    orderLineItem.setOrder(order);
    orderLineItem.setOrderedQuantity(5L);

    assertNull(orderLineItem.getId());

    orderLineItem = orderLineItemRepository.save(orderLineItem);
    assertNotNull(orderLineItem.getId());
  }
}
