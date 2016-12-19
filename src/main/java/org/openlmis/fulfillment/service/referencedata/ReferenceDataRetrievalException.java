package org.openlmis.fulfillment.service.referencedata;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Signals we were unable to retrieve reference data
 * due to a communication error.
 */
@Getter
public class ReferenceDataRetrievalException extends RuntimeException {
  private final String resource;
  private final String status;
  private final String response;

  /**
   * Constructs the exception.
   *
   * @param resource the resource that we were trying to retrieve
   * @param status   the http status that was returned
   * @param response the response from referencedata service
   */
  ReferenceDataRetrievalException(String resource, HttpStatus status, String response) {
    super(String.format("Unable to retrieve %s. Error code: %d, response message: %s",
        resource, status.value(), response));
    this.resource = resource;
    this.status = status.toString();
    this.response = response;
  }
}
