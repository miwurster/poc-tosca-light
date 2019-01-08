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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Objects;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.winery.security.KeystoreManager;
import org.eclipse.winery.security.SecurityProcessor;
import org.eclipse.winery.security.datatypes.KeyEntityInformation;
import org.eclipse.winery.security.exceptions.GenericKeystoreManagerException;
import org.eclipse.winery.security.exceptions.GenericSecurityProcessorException;
import org.eclipse.winery.security.support.enums.SymmetricEncryptionAlgorithmEnum;

import com.sun.jersey.multipart.FormDataParam;
import io.swagger.annotations.ApiOperation;

public class SecretKeysResource extends AbstractKeystoreEntityResource {

    public SecretKeysResource(KeystoreManager keystoreManager, SecurityProcessor securityProcessor) {
        super(keystoreManager, securityProcessor);
    }

    @ApiOperation(value = "Gets the list of secret keys")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<KeyEntityInformation> getSecretKeysList(@QueryParam("withKeyEncoded") boolean withKeyEncoded) {
        return keystoreManager.getKeys(withKeyEncoded);
    }

    @ApiOperation(value = "Generates a new or stores an existing secret key")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response storeSecretKey(@FormDataParam("algo") String algo,
                                   @FormDataParam("keySize") int keySize,
                                   @FormDataParam("keyFile") InputStream uploadedSecretKey,
                                   @Context UriInfo uriInfo) {
        try {
            if (this.parametersAreNonNull(algo)) {
                Key key;
                if (Objects.isNull(uploadedSecretKey)) {
                    key = securityProcessor.generateSecretKey(SymmetricEncryptionAlgorithmEnum.findAnyByName(algo), keySize);
                } else {
                    key = securityProcessor.getSecretKeyFromInputStream(SymmetricEncryptionAlgorithmEnum.findAnyByName(algo), uploadedSecretKey);
                }
                String alias = this.generateUniqueAlias(key);
                KeyEntityInformation entity = keystoreManager.storeKey(alias, key);
                URI uri = uriInfo.getAbsolutePathBuilder().path(alias).build();
                return Response.created(uri).entity(entity).build();
            } else {
                throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                        .entity("Insufficient number of parameters in the request")
                        .type(MediaType.TEXT_PLAIN)
                        .build()
                );
            }
        } catch (GenericKeystoreManagerException | GenericSecurityProcessorException | IllegalArgumentException | IOException | NoSuchAlgorithmException e) {
            throw new WebApplicationException(
                Response.serverError()
                    .entity(e.getMessage())
                    .type(MediaType.TEXT_PLAIN)
                    .build()
            );
        }
    }

    @ApiOperation(value = "Deletes all secret keys from the keystore")
    @DELETE
    public Response deleteAll() {
        try {
            keystoreManager.deleteAllSecretKeys();
        } catch (GenericKeystoreManagerException e) {
            throw new WebApplicationException(
                Response.serverError().entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build()
            );
        }
        return Response.noContent().build();
    }

    @Path("{alias}")
    public SecretKeyResource getSecretKeyResource() {
        return new SecretKeyResource(keystoreManager, securityProcessor);
    }
}
