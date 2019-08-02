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

package org.openlmis.fulfillment.service.referencedata;

import static java.lang.Boolean.parseBoolean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.fulfillment.web.util.BaseDto;
import org.openlmis.fulfillment.web.util.MetadataDto;
import org.openlmis.fulfillment.web.util.VersionIdentityDto;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderableDto extends BaseDto {

  private static final String USE_VVM = "useVVM";

  private String productCode;
  private String fullProductName;
  private long netContent;
  private long packRoundingThreshold;
  private boolean roundToZero;
  private Set<ProgramOrderableDto> programs;
  private DispensableDto dispensable;
  private Map<String, String> extraData;
  private MetadataDto meta = new MetadataDto();

  @JsonIgnore
  public boolean useVvm() {
    return null != extraData && parseBoolean(extraData.get(USE_VVM));
  }

  @JsonIgnore
  public Long getVersionNumber() {
    return Long.valueOf(meta.getVersionNumber());
  }

  @JsonIgnore
  public VersionIdentityDto getIdentity() {
    return new VersionIdentityDto(getId(), getVersionNumber());
  }
}
