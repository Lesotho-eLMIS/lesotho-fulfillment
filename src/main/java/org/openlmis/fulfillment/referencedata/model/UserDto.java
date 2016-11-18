package org.openlmis.fulfillment.referencedata.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class UserDto {
  private UUID id;
  private String username;
  private String firstName;
  private String lastName;
  private String email;
  private boolean verified;
  private FacilityDto homeFacility;
  private Set<RoleAssignmentDto> roleAssignments;
}
