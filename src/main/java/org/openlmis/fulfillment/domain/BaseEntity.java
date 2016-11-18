package org.openlmis.fulfillment.domain;

import com.fasterxml.jackson.annotation.JsonView;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseEntity {

  @Id
  @GeneratedValue(generator = "uuid-gen")
  @GenericGenerator(name = "uuid-gen", strategy = "uuid2")
  @JsonView(View.BasicInformation.class)
  @Type(type = "pg-uuid")
  @Getter
  @Setter
  protected UUID id;
}
