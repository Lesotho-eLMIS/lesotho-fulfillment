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

package org.openlmis.fulfillment.repository.custom;

import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface OrderRepositoryCustom {

  Page<Order> searchOrders(UUID supplyingFacility, UUID requestingFacility, UUID program,
      UUID processingPeriod, Set<OrderStatus> statuses, Pageable pageable,
      Set<UUID> availableSupplyingFacilities, Set<UUID> availableRequestingFacilities);

  List<UUID> getRequestingFacilities(List<UUID> supplyingFacilityIds);
}
