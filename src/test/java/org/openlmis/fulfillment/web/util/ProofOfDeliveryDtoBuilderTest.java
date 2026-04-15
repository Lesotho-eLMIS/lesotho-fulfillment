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
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.fulfillment.web.util;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openlmis.fulfillment.ProofOfDeliveryDataBuilder;
import org.openlmis.fulfillment.ProofOfDeliveryLineItemDataBuilder;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.ShipmentQuantityType;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.service.referencedata.OrderableReferenceDataService;
import org.openlmis.fulfillment.testutils.OrderableDataBuilder;
import org.openlmis.fulfillment.testutils.ShipmentDataBuilder;
import org.openlmis.fulfillment.testutils.ShipmentLineItemDataBuilder;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class ProofOfDeliveryDtoBuilderTest {

  @Mock
  private OrderableReferenceDataService orderableReferenceDataService;

  @InjectMocks
  private ProofOfDeliveryDtoBuilder proofOfDeliveryDtoBuilder;

  @Before
  public void setUp() {
    ReflectionTestUtils.setField(proofOfDeliveryDtoBuilder, "serviceUrl", "http://localhost");
    when(orderableReferenceDataService.findByIdentities(anySet())).thenAnswer(
        invocation -> ((java.util.Set<VersionEntityReference>) invocation.getArgument(0)).stream()
            .map(identity -> new OrderableDataBuilder()
                .withId(identity.getId())
                .withVersionNumber(identity.getVersionNumber())
                .build())
            .collect(Collectors.toList()));
  }

  @Test
  public void shouldSetQuantityTypeFromShipmentLineItems() {
    UUID orderableId = UUID.randomUUID();
    Long versionNumber = 1L;
    UUID lotId = UUID.randomUUID();
    Shipment shipment = new ShipmentDataBuilder()
        .withLineItems(singletonList(new ShipmentLineItemDataBuilder()
            .withOrderable(orderableId, versionNumber)
            .withLotId(lotId)
            .withQuantityType(ShipmentQuantityType.DISPENSING_UNITS)
            .build()))
        .build();
    ProofOfDeliveryLineItem podLineItem = new ProofOfDeliveryLineItemDataBuilder()
        .withOrderable(orderableId, versionNumber)
        .withoutQuantities()
        .withoutReason()
        .withoutVvmStatus()
        .build();
    ReflectionTestUtils.setField(podLineItem, "lotId", lotId);
    ProofOfDelivery proofOfDelivery = new ProofOfDeliveryDataBuilder()
        .withShipment(shipment)
        .withLineItems(singletonList(podLineItem))
        .build();

    ProofOfDeliveryDto dto = proofOfDeliveryDtoBuilder.build(proofOfDelivery);

    assertEquals(ShipmentQuantityType.DISPENSING_UNITS,
        dto.lineItems().get(0).getQuantityType());
  }

  @Test
  public void shouldDefaultQuantityTypeToPacksWhenShipmentLineCannotBeMatched() {
    UUID orderableId = UUID.randomUUID();
    Long versionNumber = 1L;
    Shipment shipment = new ShipmentDataBuilder().build();
    ProofOfDeliveryLineItem podLineItem = new ProofOfDeliveryLineItemDataBuilder()
        .withOrderable(orderableId, versionNumber)
        .withoutQuantities()
        .withoutReason()
        .withoutVvmStatus()
        .build();
    ProofOfDelivery proofOfDelivery = new ProofOfDeliveryDataBuilder()
        .withShipment(shipment)
        .withLineItems(singletonList(podLineItem))
        .build();

    ProofOfDeliveryDto dto = proofOfDeliveryDtoBuilder.build(proofOfDelivery);

    assertEquals(ShipmentQuantityType.PACKS, dto.lineItems().get(0).getQuantityType());
  }
}
