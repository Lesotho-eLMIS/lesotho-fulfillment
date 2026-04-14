/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.fulfillment.web.shipmentdraft;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openlmis.fulfillment.OrderDataBuilder;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.ShipmentDraft;
import org.openlmis.fulfillment.domain.UpdateDetails;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.repository.ShipmentDraftRepository;
import org.openlmis.fulfillment.service.PermissionService;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.util.DateHelper;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDtoDataBuilder;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;

@SuppressWarnings("PMD.UnusedPrivateField")
public class ShipmentDraftControllerTest {

  @Mock
  private ShipmentDraftRepository repository;

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private ShipmentDraftDtoBuilder draftDtoBuilder;

  @Mock
  private PermissionService permissionService;

  @Mock
  private AuthenticationHelper authenticationHelper;

  @Mock
  private DateHelper dateHelper;

  @InjectMocks
  private ShipmentDraftController shipmentDraftController = new ShipmentDraftController();

  private ShipmentDraftDto shipmentDraftDto;
  private ShipmentDraft shipmentDraft;
  private Order order;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    ShipmentLineItemDto lineItem = new ShipmentLineItemDtoDataBuilder().withoutLot().build();
    shipmentDraftDto = new ShipmentDraftDtoDataBuilder()
        .withOrder(new OrderObjectReferenceDto(UUID.randomUUID()))
        .withLineItems(java.util.Collections.singletonList(lineItem))
        .build();

    order = new OrderDataBuilder().withOrderedStatus().build();
    order.setId(shipmentDraftDto.getOrder().getId());
    shipmentDraft = ShipmentDraft.newInstance(shipmentDraftDto);

    when(orderRepository.findById(shipmentDraftDto.getOrder().getId()))
        .thenReturn(Optional.of(order));
    when(repository.save(any(ShipmentDraft.class))).thenReturn(shipmentDraft);
    when(draftDtoBuilder.build(any(ShipmentDraft.class))).thenReturn(shipmentDraftDto);
    when(dateHelper.getCurrentDateTimeWithSystemZone()).thenReturn(ZonedDateTime.now());
  }

  @Test
  public void shouldFallbackToOrderUpdaterWhenCurrentUserIsMissing() {
    UUID fallbackUserId = UUID.randomUUID();
    order.setUpdateDetails(new UpdateDetails(fallbackUserId, ZonedDateTime.now()));
    when(authenticationHelper.getCurrentUser()).thenReturn(null);

    shipmentDraftController.createShipmentDraft(shipmentDraftDto);

    ArgumentCaptor<Order> argument = ArgumentCaptor.forClass(Order.class);
    verify(orderRepository).save(argument.capture());
    assertEquals(OrderStatus.FULFILLING, argument.getValue().getStatus());
    assertEquals(fallbackUserId, argument.getValue().getUpdateDetails().getUpdaterId());
  }
}
