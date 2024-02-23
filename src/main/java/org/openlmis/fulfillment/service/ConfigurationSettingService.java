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

package org.openlmis.fulfillment.service;

import java.util.UUID;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@NoArgsConstructor
public class ConfigurationSettingService {

  private static final String RESONS_SUFFIX = "reasons.";
  static final String TRANSFER_IN = RESONS_SUFFIX + "transferIn";
  static final String INTERNAL_TRANSFER = RESONS_SUFFIX + "internalTransfer";
  static final String TRANSFER_OUT = RESONS_SUFFIX + "transferOut";
  private static final String FTP_TRANSFER = "ftp.transfer.on.requisition.to.order";
  private static final String SEND_EMAIL = "send.email.on.requisition.to.order";

  @Autowired
  private Environment env;

  public UUID getTransferInReasonId() {
    return UUID.fromString(env.getProperty(TRANSFER_IN));
  }

  public UUID getInternalTransferId() {
    return UUID.fromString(env.getProperty(INTERNAL_TRANSFER));
  }

  public UUID getTransferOutReasonId() {
    return UUID.fromString(env.getProperty(TRANSFER_OUT));
  }

  public String getAllowFtpTransferOnRequisitionToOrder() {
    return env.getProperty(FTP_TRANSFER);
  }

  public String getAllowSendingEmailOnRequisitionToOrder() {
    return env.getProperty(SEND_EMAIL);
  }

}
