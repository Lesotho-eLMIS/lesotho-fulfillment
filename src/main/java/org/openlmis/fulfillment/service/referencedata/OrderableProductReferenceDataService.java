package org.openlmis.fulfillment.service.referencedata;

import org.springframework.stereotype.Service;

@Service
public class OrderableProductReferenceDataService
    extends BaseReferenceDataService<OrderableProductDto> {

  @Override
  protected String getUrl() {
    return "/api/orderableProducts/";
  }

  @Override
  protected Class<OrderableProductDto> getResultClass() {
    return OrderableProductDto.class;
  }

  @Override
  protected Class<OrderableProductDto[]> getArrayResultClass() {
    return OrderableProductDto[].class;
  }
}
