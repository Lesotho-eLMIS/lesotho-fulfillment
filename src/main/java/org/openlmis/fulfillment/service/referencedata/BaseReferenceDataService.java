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

import org.openlmis.fulfillment.service.BaseCommunicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class BaseReferenceDataService<T> extends BaseCommunicationService<T> {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Value("${referencedata.url}")
  private String referenceDataUrl;

  /**
   * Return one object from Reference data service.
   *
   * @param id UUID of requesting object.
   * @return Requesting reference data object.
   */
  public T findOne(UUID id) {
    String url = getServiceUrl() + getUrl() + id;

    Map<String, String> params = new HashMap<>();
    params.put(ACCESS_TOKEN, obtainAccessToken());

    try {
      ResponseEntity<T> responseEntity = restTemplate.exchange(
          buildUri(url, params), HttpMethod.GET, null, getResultClass());
      return responseEntity.getBody();
    } catch (HttpStatusCodeException ex) {
      // rest template will handle 404 as an exception, instead of returning null
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
        logger.warn("{} with id {} does not exist. ", getResultClass().getSimpleName(), id);
        return null;
      } else {
        throw buildRefDataException(ex);
      }
    }
  }

  /**
   * Return all reference data T objects.
   *
   * @param resourceUrl Endpoint url.
   * @param parameters  Map of query parameters.
   * @return all reference data T objects.
   */
  Collection<T> findAll(String resourceUrl, Map<String, Object> parameters) {
    return findAllWithMethod(resourceUrl, parameters, null, HttpMethod.GET);
  }

  /**
   * Return all reference data T objects that need to be retrieved with POST request.
   *
   * @param resourceUrl   Endpoint url.
   * @param uriParameters Map of query parameters.
   * @param payload       body to include with the outgoing request.
   * @return all reference data T objects.
   */
  Collection<T> postFindAll(String resourceUrl, Map<String, Object> uriParameters,
                                   Map<String, Object> payload) {
    return findAllWithMethod(resourceUrl, uriParameters, payload, HttpMethod.POST);
  }

  private Collection<T> findAllWithMethod(String resourceUrl, Map<String, Object> uriParameters,
                                          Map<String, Object> payload, HttpMethod method) {
    String url = getServiceUrl() + getUrl() + resourceUrl;

    Map<String, Object> params = new HashMap<>();
    params.put(ACCESS_TOKEN, obtainAccessToken());
    params.putAll(uriParameters);

    try {
      ResponseEntity<T[]> responseEntity;
      if (HttpMethod.GET == method) {
        responseEntity = restTemplate.getForEntity(buildUri(url, params), getArrayResultClass());
      } else {
        responseEntity = restTemplate.postForEntity(buildUri(url, params), payload,
            getArrayResultClass());
      }

      return new ArrayList<>(Arrays.asList(responseEntity.getBody()));
    } catch (HttpStatusCodeException ex) {
      throw buildRefDataException(ex);
    }
  }

  <P> P get(Class<P> type, String resourceUrl, Map<String, Object> parameters) {
    String url = getServiceUrl() + getUrl() + resourceUrl;
    Map<String, Object> params = new HashMap<>();
    params.put(ACCESS_TOKEN, obtainAccessToken());
    params.putAll(parameters);

    ResponseEntity<P> response = restTemplate.getForEntity(buildUri(url, params), type);

    return response.getBody();
  }

  protected String getServiceUrl() {
    return referenceDataUrl;
  }

  protected abstract String getUrl();

  protected abstract Class<T> getResultClass();

  protected abstract Class<T[]> getArrayResultClass();

  private ReferenceDataRetrievalException buildRefDataException(HttpStatusCodeException ex) {
    return new ReferenceDataRetrievalException(
        getResultClass().getSimpleName(), ex.getStatusCode(), ex.getResponseBodyAsString()
    );
  }
}
