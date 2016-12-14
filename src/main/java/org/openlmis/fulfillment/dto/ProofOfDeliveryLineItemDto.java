package org.openlmis.fulfillment.dto;


import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProofOfDeliveryLineItemDto implements ProofOfDeliveryLineItem.Importer,
    ProofOfDeliveryLineItem.Exporter {

  private UUID id;
  private OrderLineItemDto orderLineItem;
  private Long packToShip;
  private Long quantityShipped;
  private Long quantityReceived;
  private Long quantityReturned;
  private String replacedProductCode;
  private String notes;

}
