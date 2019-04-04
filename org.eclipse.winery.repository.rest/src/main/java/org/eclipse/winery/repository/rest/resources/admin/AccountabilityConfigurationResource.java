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
package org.eclipse.winery.repository.rest.resources.admin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.eclipse.winery.accountability.AccountabilityManager;
import org.eclipse.winery.accountability.AccountabilityManagerFactory;
import org.eclipse.winery.accountability.exceptions.AccountabilityException;
import org.eclipse.winery.accountability.exceptions.BlockchainException;
import org.eclipse.winery.common.constants.MimeTypes;
import org.eclipse.winery.repository.backend.AccountabilityConfigurationManager;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.rest.resources.apiData.AccountabilityConfigurationData;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountabilityConfigurationResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountabilityConfigurationResource.class);

    private static AccountabilityManager getAccountabilityManager() throws AccountabilityException {
        Properties props = RepositoryFactory.getRepository().getAccountabilityConfigurationManager().properties;

        return AccountabilityManagerFactory.getAccountabilityManager(props);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccountabilityConfiguration() {
        Properties props = RepositoryFactory.getRepository().getAccountabilityConfigurationManager().properties;
        AccountabilityConfigurationData result = new AccountabilityConfigurationData();
        result.setAuthorizationSmartContractAddress(props.getProperty("ethereum-authorization-smart-contract-address"));
        result.setProvenanceSmartContractAddress(props.getProperty("ethereum-provenance-smart-contract-address"));
        result.setPermissionsSmartContractAddress(props.getProperty("ethereum-permissions-smart-contract-address"));
        result.setBlockchainNodeUrl(props.getProperty("geth-url"));
        result.setActiveKeystore(props.getProperty("ethereum-credentials-file-name"));
        result.setKeystorePassword(props.getProperty("ethereum-password"));
        result.setSwarmGatewayUrl(props.getProperty("swarm-gateway-url"));

        return Response.ok(result).build();
    }

    @PUT
    @Consumes( {MediaType.MULTIPART_FORM_DATA})
    public Response setAccountabilityConfiguration(
        @FormDataParam("keystoreFile") InputStream keystoreFileStream,
        @FormDataParam("keystoreFile") FormDataContentDisposition disposition,
        @FormDataParam("blockhainNodeUrl") String blockchainNodeUrl,
        @FormDataParam("keystorePassword") String keystorePassword,
        @FormDataParam("authorizationSmartContractAddress") String authorizationSmartContractAddress,
        @FormDataParam("provenanceSmartContractAddress") String provenanceSmartContractAddress,
        @FormDataParam("permissionsSmartContractAddress") String permissionsSmartContractAddress,
        @FormDataParam("swarmGatewayUrl") String swarmGatewayUrl
    ) {
        AccountabilityConfigurationManager manager = RepositoryFactory.getRepository().getAccountabilityConfigurationManager();
        try {

            // sending a new keystore file is optional
            if (keystoreFileStream != null && disposition != null) {
                manager.setNewKeystoreFile(keystoreFileStream, disposition.getFileName());
            }
            Properties props = manager.properties;
            props.setProperty("ethereum-authorization-smart-contract-address", authorizationSmartContractAddress);
            props.setProperty("ethereum-provenance-smart-contract-address", provenanceSmartContractAddress);
            props.setProperty("ethereum-permissions-smart-contract-address", permissionsSmartContractAddress);
            props.setProperty("geth-url", blockchainNodeUrl);
            props.setProperty("ethereum-password", keystorePassword);
            props.setProperty("swarm-gateway-url", swarmGatewayUrl);

            manager.saveProperties();
            return Response.noContent().build();
        } catch (IOException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/authorizationSC")
    public Response deployAuthorizationSmartContract() {
        try {
            final Properties props = RepositoryFactory.getRepository().getAccountabilityConfigurationManager().properties;
            final String address = AccountabilityManagerFactory.getAccountabilityManager(props).deployAuthorizationSmartContract().get();

            return Response.ok(address).build();
        } catch (AccountabilityException | InterruptedException | ExecutionException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/provenanceSC")
    public Response deployProvenanceSmartContract() {
        try {
            final Properties props = RepositoryFactory.getRepository().getAccountabilityConfigurationManager().properties;
            final String address = AccountabilityManagerFactory.getAccountabilityManager(props).deployProvenanceSmartContract().get();

            return Response.ok(address).build();
        } catch (AccountabilityException | InterruptedException | ExecutionException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/permissionsSC")
    public Response deployPermissionsSmartContract() {
        try {
            final Properties props = RepositoryFactory.getRepository().getAccountabilityConfigurationManager().properties;
            final String address = AccountabilityManagerFactory.getAccountabilityManager(props).deployPermissionsSmartContract().get();

            return Response.ok(address).build();
        } catch (AccountabilityException | InterruptedException | ExecutionException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    // the password is transmitted as a plain query parameter which is (probably) subject to injection
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/createKeystore")
    public Response createNewKeystoreFile(@QueryParam("keystorePassword") String password) {
        try {
            if (Objects.isNull(password)) {
                return Response.serverError().entity("Password cannot be passed empty").build();
            }

            final Properties props = RepositoryFactory.getRepository().getAccountabilityConfigurationManager().properties;
            final java.nio.file.Path filePath = AccountabilityManagerFactory.getAccountabilityManager(props).createNewKeystore(password);

            StreamingOutput so = output -> {
                try {
                    Files.copy(filePath, output);
                } catch (IOException e) {
                    LOGGER.error("Error while downloading generated keystore file", e);
                    throw new WebApplicationException(e);
                }
            };

            String contentDisposition = String.format("attachment;filename=\"%s\"",
                filePath.getFileName());

            return Response.ok()
                .header("Content-Disposition", contentDisposition)
                .type(MimeTypes.MIMETYPE_JSON)
                .entity(so)
                .build();
        } catch (BlockchainException | AccountabilityException e) {
            // todo exposing this error message might be unsafe!
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/identity")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getMyIdentity() {
        try {
            return Response.ok().entity(getAccountabilityManager().getMyIdentity()).build();
        } catch (AccountabilityException e) {
            String msg = String.format("Cannot retrieve identity. Reason: %s", e.getMessage());
            LOGGER.error(msg);
            return Response.serverError().entity(msg).build();
        }
    }

    @DELETE
    public Response restoreDefaults() {
        AccountabilityConfigurationManager manager = RepositoryFactory.getRepository().getAccountabilityConfigurationManager();
        try {
            manager.restoreDefaults();

            return Response.noContent().build();
        } catch (IOException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}
