package org.openlmis.order.referencedata.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ProgramProductDto {
  private UUID programId;
  private UUID productId;
  private UUID productCategoryId;
  private String productCategoryDisplayName;
  private Integer productCategoryDisplayOrder;
  private Boolean active;
  private Boolean fullSupply;
  private Integer displayOrder;
  private Integer maxMonthsOfStock;
  private Integer dosesPerMonth;
  private MoneyDto value;
}
