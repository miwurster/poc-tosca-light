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

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.eclipse.winery.model.tosca.constants.QNames;
import org.eclipse.winery.repository.rest.RestUtils;
import org.eclipse.winery.repository.rest.resources.apiData.QNameWithTypeApiData;
import org.eclipse.winery.security.KeystoreManager;
import org.eclipse.winery.security.SecurityProcessor;
import org.eclipse.winery.security.datatypes.KeyEntityInformation;
import org.eclipse.winery.security.datatypes.KeyPairInformation;
import org.eclipse.winery.security.datatypes.KeyType;
import org.eclipse.winery.security.exceptions.GenericKeystoreManagerException;

import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.winery.repository.security.csar.SecureCSARConstants.MASTER_SIGNING_KEYNAME;

public class KeyPairResource extends AbstractKeyPairResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyPairResource.class);
    
    public KeyPairResource(KeystoreManager keystoreManager, SecurityProcessor securityProcessor) {
        super(keystoreManager, securityProcessor);
    }

    @ApiOperation(value = "Sets the keypair as the master signing keypair")
    @PUT
    @Path("setmaster")
    public Response setAsMaster(@PathParam("alias") String alias) {
        String newName = this.renameOldMaster();

        if(!Objects.isNull(newName))
            LOGGER.info("The old master signing keypair was renamed to: " + newName);

        this.renameKeyPair(alias, MASTER_SIGNING_KEYNAME);

        return Response.ok().build();
    }

    @ApiOperation(value = "Gets the keypair by its alias")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getKeyPairInfo(@PathParam("alias") String alias) {
        try {
            String preparedAlias = prepareAlias(alias);
            return Response.ok()
                .entity(this.keystoreManager.getKeyPairData(preparedAlias))
                .build();
        } catch (GenericKeystoreManagerException e) {
            throw new WebApplicationException(
                Response.serverError().entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build()
            );
        }
    }

    @ApiOperation(value = "Gets the private key of a keypair")
    @GET
    @Path("privatekey")
    @Produces( {MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
    public Response getPrivateKeyInfo(@PathParam("alias") String alias, @QueryParam("toFile") boolean asFile) {
        String preparedAlias = prepareAlias(alias);
        try {
            if (!asFile) {
                KeyEntityInformation key = this.keystoreManager.getKey(preparedAlias, KeyType.PRIVATE);
                return Response.ok().entity(key).build();
            } else {
                byte[] key = keystoreManager.getKeyEncoded(preparedAlias, KeyType.PRIVATE);
                StreamingOutput stream = keyToStreamingOutput(key);
                return Response.ok(stream)
                    .header("content-disposition", "attachment; filename = " + preparedAlias + "." + KeyType.PRIVATE + ".key")
                    .type(MediaType.APPLICATION_OCTET_STREAM)
                    .build();
            }
        } catch (GenericKeystoreManagerException e) {
            throw new WebApplicationException(
                Response.serverError().entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build()
            );
        }
    }

    @ApiOperation(value = "Gets the public key of a keypair")
    @GET
    @Path("publickey")
    @Produces( {MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
    public Response getPublicKeyInfo(@PathParam("alias") String alias, @QueryParam("toFile") boolean asFile) {
        String preparedAlias = prepareAlias(alias);
        try {
            if (!asFile) {
                KeyEntityInformation key = this.keystoreManager.getKey(preparedAlias, KeyType.PUBLIC);
                return Response.ok().entity(key).type(MediaType.TEXT_PLAIN).build();
            } else {
                byte[] key = keystoreManager.getKeyEncoded(preparedAlias, KeyType.PUBLIC);
                StreamingOutput stream = keyToStreamingOutput(key);
                return Response.ok(stream)
                    .header("content-disposition", "attachment; filename = " + preparedAlias + "." + KeyType.PUBLIC + ".key")
                    .type(MediaType.APPLICATION_OCTET_STREAM)
                    .build();
            }
        } catch (GenericKeystoreManagerException e) {
            throw new WebApplicationException(
                Response.serverError().entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build()
            );
        }
    }

    @ApiOperation(value = "Gets certificates of a keypair")
    @GET
    @Path("certificates")
    @Produces( {MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
    public Response getCertificateInfo(@PathParam("alias") String alias, @QueryParam("toFile") boolean asFile) {
        String preparedAlias = prepareAlias(alias);
        try {
            if (!asFile) {
                String certInfo = this.keystoreManager.getPEMCertificateChain(preparedAlias);
                return Response.ok().entity(certInfo).type(MediaType.TEXT_PLAIN).build();
            } else {
                byte[] cert = keystoreManager.getCertificateEncoded(preparedAlias);
                StreamingOutput stream = keyToStreamingOutput(cert);
                return Response.ok(stream)
                    .header("content-disposition", "attachment; filename = " + preparedAlias + ".crt")
                    .type(MediaType.APPLICATION_OCTET_STREAM)
                    .build();
            }
        } catch (GenericKeystoreManagerException e) {
            throw new WebApplicationException(
                Response.serverError().entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build()
            );
        }
    }

    @ApiOperation(value = "Generates a body of a Policy Template for chosen secret key")
    @GET
    @Path("/signingpolicy")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSigningPolicyTemplate(@PathParam("alias") String alias) {
        String preparedAlias = prepareAlias(alias);
        if (!this.keystoreManager.entityExists(preparedAlias))
            return Response.status(Response.Status.NOT_FOUND).build();

        return RestUtils.generateSecurityPolicyTemplateBody(alias, QNames.WINERY_SIGNING_POLICY_TYPE);
    }

    @ApiOperation(value = "Generates a Policy Template for chosen secret key")
    @PUT
    @Path("/signingpolicy")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveSigningPolicyTemplate(QNameWithTypeApiData jsonData) {
        String preparedAlias = prepareAlias(jsonData.localname);
        if (!this.keystoreManager.entityExists(preparedAlias))
            return Response.status(Response.Status.NOT_FOUND).build();

        try {
            KeyPairInformation kp = this.keystoreManager.getKeyPairData(preparedAlias);
            RestUtils.createSigningPolicyTemplate(jsonData, kp);
        } catch (GenericKeystoreManagerException e) {
            throw new WebApplicationException(
                Response.serverError().entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build()
            );
        }

        return Response.ok().build();
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
