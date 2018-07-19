/********************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *******************************************************************************/

package org.eclipse.winery.repository.rest.resources.servicetemplates.propertiesdefinition;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.winery.model.tosca.TBoundaryDefinitions;
import org.eclipse.winery.model.tosca.kvproperties.PropertyDefinitionKV;
import org.eclipse.winery.repository.backend.NamespaceManager;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.backend.xsd.NamespaceAndDefinedLocalNames;
import org.eclipse.winery.repository.rest.RestUtils;
import org.eclipse.winery.repository.rest.datatypes.NamespaceAndDefinedLocalNamesForAngular;
import org.eclipse.winery.repository.rest.resources.apiData.PropertiesDefinitionEnum;
import org.eclipse.winery.repository.rest.resources.apiData.PropertiesDefinitionResourceApiData;
import org.eclipse.winery.repository.rest.resources.servicetemplates.ServiceTemplateResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class BoundaryDefsPropertiesDefinitionResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(BoundaryDefsPropertiesDefinitionResource.class);

    private final ServiceTemplateResource parentRes;

    public BoundaryDefsPropertiesDefinitionResource(ServiceTemplateResource res) {
        this.parentRes = res;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public PropertiesDefinitionResourceApiData getJson() {
        Object definition = parentRes.getServiceTemplate().getPropertiesDefinition();
        return new PropertiesDefinitionResourceApiData(definition);
    }

    @GET
    @Path("{type}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<NamespaceAndDefinedLocalNamesForAngular> getXsdDefinitionJson(@PathParam("type") String type) {
        List<NamespaceAndDefinedLocalNames> allDeclaredElementsLocalNames = null;
        switch (type) {
            case "type":
                allDeclaredElementsLocalNames = RepositoryFactory.getRepository().getXsdImportManager().getAllDefinedTypesLocalNames();
                break;
            case "element":
                allDeclaredElementsLocalNames = RepositoryFactory.getRepository().getXsdImportManager().getAllDeclaredElementsLocalNames();
                break;
        }

        if (allDeclaredElementsLocalNames == null) {
            LOGGER.error("No such parameter available in this call", type);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        return RestUtils.convert(allDeclaredElementsLocalNames);
    }

    @DELETE
    public Response clearPropertiesDefinition() {
        parentRes.getServiceTemplate().setPropertiesDefinition(null);
        return RestUtils.persist(this.parentRes);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response onJsonPost(PropertiesDefinitionResourceApiData data) {
        if (data.selectedValue == PropertiesDefinitionEnum.Element || data.selectedValue == PropertiesDefinitionEnum.Type) {
            // TODO
            // return RestUtils.persist(this.parentRes);
        } else if (data.selectedValue == PropertiesDefinitionEnum.Custom) {
            parentRes.getServiceTemplate().setPropertiesDefinition(data.winerysPropertiesDefinition);
            String namespace = data.winerysPropertiesDefinition.getNamespace();
            NamespaceManager namespaceManager = RepositoryFactory.getRepository().getNamespaceManager();
            if (!namespaceManager.hasPermanentPrefix(namespace)) {
                namespaceManager.addPermanentNamespace(namespace);
            }

            // BackendUtils.initializeProperties(repository, (TEntityTemplate) element);
            Document document;
            try {
                document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            } catch (ParserConfigurationException e) {
                LOGGER.error("Could not create document", e);
                return Response.serverError().build();
            }

            final String ns = data.winerysPropertiesDefinition.getNamespace();
            final Element wrapperElement = document.createElementNS(ns, data.winerysPropertiesDefinition.getElementName());
            document.appendChild(wrapperElement);

            // we produce the serialization in the same order the XSD would be generated (because of the usage of xsd:sequence)
            for (PropertyDefinitionKV propertyDefinitionKV : data.winerysPropertiesDefinition.getPropertyDefinitionKVList()) {
                // we always write the element tag as the XSD forces that
                final Element valueElement = document.createElementNS(ns, propertyDefinitionKV.getKey());
                wrapperElement.appendChild(valueElement);
            }

            TBoundaryDefinitions.Properties properties = new TBoundaryDefinitions.Properties();
            properties.setAny(document.getDocumentElement());
            parentRes.getServiceTemplate().setBoundaryDefinitions(new TBoundaryDefinitions());
            parentRes.getServiceTemplate().getBoundaryDefinitions().setProperties(properties);

            return RestUtils.persist(this.parentRes);
        }

        return Response.status(Status.BAD_REQUEST).entity("Wrong data submitted!").build();
    }
}
