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

package org.eclipse.winery.repository.rest.resources.admin.keystore;

import java.util.Objects;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.winery.security.KeystoreManager;
import org.eclipse.winery.security.SecurityProcessor;
import org.eclipse.winery.security.exceptions.GenericKeystoreManagerException;

import io.swagger.annotations.ApiOperation;

public class CertificateResource extends AbstractKeystoreEntityResource {

    public CertificateResource(KeystoreManager keystoreManager, SecurityProcessor securityProcessor) {
        super(keystoreManager, securityProcessor);
    }

    @ApiOperation(value = "Gets the keypair by its alias")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCertificateInfo(@PathParam("alias") String alias,
                                       @QueryParam(value = "pemencoded") String pemencoded) {
        try {
            String preparedAlias = prepareAlias(alias);
            if (Objects.nonNull(pemencoded)) {
                return Response.ok()
                    .entity(this.keystoreManager.getPEMCertificateChain(preparedAlias))
                    .build();
            }
            return Response.ok()
                .entity(this.keystoreManager.getCertificate(preparedAlias))
                .build();
        } catch (GenericKeystoreManagerException e) {
            throw new WebApplicationException(
                Response.serverError().entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build()
            );
        }
    }

    @ApiOperation(value = "Deletes resource using its alias")
    @DELETE
    public Response deleteEntity(@PathParam("alias") String alias) {
        String preparedAlias = prepareAlias(alias);
        if (!this.keystoreManager.entityExists(preparedAlias))
            return Response.status(Response.Status.NOT_FOUND).build();
        try {
            this.keystoreManager.deleteKeystoreEntry(preparedAlias);
        } catch (GenericKeystoreManagerException e) {
            throw new WebApplicationException(
                Response.serverError().entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build()
            );
        }
        return Response.noContent().build();
    }
    
}
