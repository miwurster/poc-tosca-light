/********************************************************************************
 * Copyright (c) 2018-2019 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.repository.rest.resources.admin.keypermissions;

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.winery.common.ids.admin.ACListId;
import org.eclipse.winery.repository.rest.resources.admin.AbstractAdminResource;
import org.eclipse.winery.repository.security.csar.PartnerACListProcessor;

import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessControlListAdminResource extends AbstractAdminResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessControlListAdminResource.class);

    public AccessControlListAdminResource() {
        super(new ACListId());
    }

    @ApiOperation(value = "Gets the list of all participant-policy permissions")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response listKeyPermissions() {
        return Response.ok("dummy-return", MediaType.TEXT_PLAIN).build();
    }

    @ApiOperation(value = "Generates a new partner ACL")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPartner(@FormParam("partner") String partnerName,
                                  @Context UriInfo uriInfo) {
        try {
            PartnerACListProcessor.createPartnerACL(partnerName);
            URI uri = uriInfo.getAbsolutePathBuilder().path(partnerName).build();
            return Response.created(uri).build();
        }
        catch (WebApplicationException e) {
            LOGGER.error("Error generating a new partner access control list", e.getMessage());
            throw e;
        }        
    }

    @Path("{partnerName}")
    public KeyExchangePartnerResource getKeyExchangePartnerResource(@PathParam("partnerName") String partnerName) {
        return new KeyExchangePartnerResource(partnerName);
    }
    
}
