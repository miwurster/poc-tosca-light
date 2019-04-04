/********************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.winery.accountability.exceptions.AccountabilityException;
import org.eclipse.winery.accountability.exceptions.BlockchainException;
import org.eclipse.winery.accountability.exceptions.ParticipantPublicKeyNotSetException;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.security.csar.KeyAssignments;
import org.eclipse.winery.repository.security.csar.PermissionsManager;
import org.eclipse.winery.security.exceptions.GenericKeystoreManagerException;

import com.sun.jersey.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyExchangeResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyExchangeResource.class);

    private static Response createException(String msg, Exception e) {
        final String cause = String.format("%s. Reason: %s", msg, e.getMessage());
        LOGGER.error(cause);
        return Response.serverError().entity(cause).build();
    }

    private static Response createNoPublicKeyWarning() {
        final String msg = "The official public key of the participant is not set";
        return Response.status(Response.Status.NOT_FOUND).entity(msg).build();
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateListOfKeysGivenToMe() {
        try {
            PermissionsManager manager = RepositoryFactory.getRepository().getPermissionsManager();

            return Response.ok().entity(manager.updateListOfPermissionsGivenToMe().get()).build();
        } catch (AccountabilityException | BlockchainException | InterruptedException | ExecutionException e) {
            return createException("Could not update list of keys given to the current user", e);
        }
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response givePermission(@FormDataParam("address") String recipientAddress, @FormDataParam("alias") String secretKeyAlias) {
        try {
            PermissionsManager manager = RepositoryFactory.getRepository().getPermissionsManager();
            manager.givePermission(recipientAddress, secretKeyAlias).get();

            return Response.ok().build();
        } catch (InterruptedException | AccountabilityException | GenericKeystoreManagerException | BlockchainException | ExecutionException | IllegalArgumentException e) {
            return createException("Could not give the permission to the desired recipient", e);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<KeyAssignments> getAllKeyAssignments() {
        PermissionsManager manager = RepositoryFactory.getRepository().getPermissionsManager();
        Map<String, KeyAssignments> map = manager.getKeyAssignments();

        return map.values();
    }

    @PUT
    @Path("publickey")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response setMyPublicKey(@FormDataParam("alias") String keyPairAlias) {
        try {
            PermissionsManager manager = RepositoryFactory.getRepository().getPermissionsManager();
            manager.setMyOfficialKeyPair(keyPairAlias).get();

            return Response.ok().build();
        } catch (InterruptedException | AccountabilityException | GenericKeystoreManagerException | BlockchainException | ExecutionException | IllegalArgumentException e) {
            return createException("Could not set the official key pair", e);
        }
    }

    @GET
    @Path("publickey")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getMyPublicKeyAlias() {
        try {
            PermissionsManager manager = RepositoryFactory.getRepository().getPermissionsManager();

            return Response.ok().entity(manager.getMyOfficialKeyPairAlias().get()).build();
        }catch (ParticipantPublicKeyNotSetException e) {
            return createNoPublicKeyWarning();
        } catch (InterruptedException | ExecutionException | AccountabilityException | BlockchainException e) {
            return createException("Could not retrieve the official key pair", e);
        }
    }
}
