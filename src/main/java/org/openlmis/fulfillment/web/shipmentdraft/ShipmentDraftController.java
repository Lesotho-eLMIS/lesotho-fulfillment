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

package org.openlmis.fulfillment.web.shipmentdraft;

import static org.openlmis.fulfillment.i18n.MessageKeys.SHIPMENT_NOT_FOUND;
import static org.openlmis.fulfillment.i18n.MessageKeys.SHIPMENT_ORDERLESS_NOT_SUPPORTED;
import static org.openlmis.fulfillment.i18n.MessageKeys.SHIPMENT_DRAFT_ORDER_NOT_FOUND;
import static org.openlmis.fulfillment.i18n.MessageKeys.SHIPMENT_DRAFT_ORDER_REQUIRED;
import static org.openlmis.fulfillment.service.ResourceNames.BASE_PATH;
import static org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftController.RESOURCE_PATH;

import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.ShipmentDraft;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.repository.ShipmentDraftRepository;
import org.openlmis.fulfillment.service.PermissionService;
import org.openlmis.fulfillment.util.Pagination;
import org.openlmis.fulfillment.web.BaseController;
import org.openlmis.fulfillment.web.NotFoundException;
import org.openlmis.fulfillment.web.ValidationException;
import org.openlmis.fulfillment.web.util.ObjectReferenceDto;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.util.List;
import java.util.UUID;

@Controller
@Transactional
@RequestMapping(RESOURCE_PATH)
public class ShipmentDraftController extends BaseController {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(ShipmentDraftController.class);

  static final String RESOURCE_PATH = BASE_PATH + "/shipmentDrafts";

  @Autowired
  private ShipmentDraftRepository repository;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private ShipmentDraftDtoBuilder draftDtoBuilder;

  @Autowired
  private PermissionService permissionService;

  /**
   * Allows creating new shipment. If the id is specified, it will be ignored.
   *
   * @param draftDto A shipment draft DTO bound to the request body.
   * @return created shipment draft.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public ShipmentDraftDto createShipmentDraft(@RequestBody ShipmentDraftDto draftDto) {
    XLOGGER.entry(draftDto);
    Profiler profiler = new Profiler("CREATE_SHIPMENT_DRAFT");
    profiler.setLogger(XLOGGER);

    ObjectReferenceDto dtoOrder = draftDto.getOrder();
    if (dtoOrder == null || dtoOrder.getId() == null) {
      throw new ValidationException(SHIPMENT_ORDERLESS_NOT_SUPPORTED);
    }

    profiler.start("CHECK_RIGHTS");
    permissionService.canEditShipmentDraft(draftDto);

    profiler.start("CREATE_DOMAIN_INSTANCE");
    ShipmentDraft draft = ShipmentDraft.newInstance(draftDto);

    profiler.start("SAVE_AND_CREATE_DTO");
    draft = repository.save(draft);
    ShipmentDraftDto dto = draftDtoBuilder.build(draft);

    profiler.stop().log();
    XLOGGER.exit(dto);
    return dto;
  }

  /**
   * Get shipment with request param.
   *
   * @param orderId order UUID (required).
   * @return a page of shipment drafts.
   */
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<ShipmentDraftDto> getShipmentDrafts(@RequestParam(required = false) UUID orderId,
                                                  Pageable pageable) {
    XLOGGER.entry(orderId);
    Profiler profiler = new Profiler("GET_SHIPMENT_DRAFTS");
    profiler.setLogger(XLOGGER);

    if (orderId == null) {
      throw new ValidationException(SHIPMENT_DRAFT_ORDER_REQUIRED);
    }

    Order order = orderRepository.findOne(orderId);
    if (order == null) {
      throw new ValidationException(SHIPMENT_DRAFT_ORDER_NOT_FOUND);
    }

    Page<ShipmentDraft> draftPage = repository.findByOrder(order, pageable);
    List<ShipmentDraftDto> draftDtos = draftDtoBuilder.build(draftPage.getContent());

    int numberOfElements = draftPage.getNumberOfElements();
    return Pagination.getPage(draftDtos, pageable, numberOfElements);
  }

  /**
   * Get chosen shipment draft.
   *
   * @param id UUID of shipment draft item which we want to get.
   * @return new instance of ShipmentDraftDto.
   */
  @GetMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ShipmentDraftDto getShipmentDraft(@PathVariable UUID id) {
    XLOGGER.entry(id);
    Profiler profiler = new Profiler("GET_SHIPMENT_DRAFT_BY_ID");
    profiler.setLogger(XLOGGER);

    profiler.start("FIND_IN_DB");
    ShipmentDraft shipment = repository.findOne(id);

    if (shipment == null) {
      throw new NotFoundException(SHIPMENT_NOT_FOUND);
    }

    profiler.start("CHECK_RIGHTS");
    permissionService.canViewShipmentDrat(shipment);

    profiler.start("CREATE_DTO");
    ShipmentDraftDto dto = draftDtoBuilder.build(shipment);

    profiler.stop().log();
    XLOGGER.exit(dto);
    return dto;
  }

}