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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.winery.common.Util;
import org.eclipse.winery.common.ids.admin.KeystoreId;
import org.eclipse.winery.model.tosca.constants.Namespaces;
import org.eclipse.winery.repository.rest.resources.admin.AbstractAdminResource;
import org.eclipse.winery.repository.security.csar.KeystoreManagerFactory;
import org.eclipse.winery.security.KeystoreManager;
import org.eclipse.winery.security.SecurityProcessor;
import org.eclipse.winery.security.SecurityProcessorFactory;
import org.eclipse.winery.security.exceptions.GenericKeystoreManagerException;
import org.eclipse.winery.security.support.AsymmetricEncryptionAlgorithmEnum;
import org.eclipse.winery.security.support.SymmetricEncryptionAlgorithmEnum;

import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyStoreAdminResource extends AbstractAdminResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreAdminResource.class);
    private final KeystoreManager keystoreManager;
    private final SecurityProcessor securityProcessor;

    public KeyStoreAdminResource() {
        super(new KeystoreId());
        keystoreManager = KeystoreManagerFactory.getInstance();

        if (!keystoreManager.keystoreExists()) {
            LOGGER.error("Error initializing keystore");
            throw new WebApplicationException(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Keystore cannot be initialized")
                    .type(MediaType.TEXT_PLAIN)
                    .build()
            );
        }
        securityProcessor = SecurityProcessorFactory.getDefaultSecurityProcessor();
    }

    @ApiOperation(value = "Gets the list of entities in the keystore",
        notes = "Returns keystore file if asFile parameter is set")
    @GET
    @Produces( {MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
    public Response listKeystoreEntities(@QueryParam("toFile") boolean asFile) {
        if (asFile) {
            // TODO: return the keystore file
            return Response.ok("dummy-return", MediaType.TEXT_PLAIN).build();
        } else {
            try {
                return Response.ok()
                    .entity(this.keystoreManager.getKeystoreContent())
                    .build();
            } catch (GenericKeystoreManagerException e) {
                throw new WebApplicationException(
                    Response.serverError()
                        .entity(e.getMessage())
                        .type(MediaType.TEXT_PLAIN)
                        .build()
                );
            }
        }
    }

    @Path("keys")
    public SecretKeysResource getSecretKeysResource() {
        return new SecretKeysResource(keystoreManager, securityProcessor);
    }

    @Path("keypairs")
    public KeyPairsResource getKeyPairsResource() {
        return new KeyPairsResource(keystoreManager, securityProcessor);
    }

    @Path("certificates")
    public CertificatesResource getCertificatesResource() {
        return new CertificatesResource(keystoreManager, securityProcessor);
    }

    @ApiOperation(value = "Gets the list of supported symmetric algorithms")
    @GET
    @Path("algorithms/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSupportedAlgorithms() {
        Map<String, Object> result = new HashMap<>();
        Collection<SymmetricEncryptionAlgorithmEnum> sym = this.keystoreManager.getSymmetricAlgorithms();
        Collection<AsymmetricEncryptionAlgorithmEnum> asym = this.keystoreManager.getAsymmetricAlgorithms();
        result.put("symmetric", sym);
        result.put("asymmetric", asym);

        return Response.ok().entity(result).build();
    }

    @ApiOperation(value = "Gets the namespace constant for security policy templates")
    @GET
    @Path("namespaces/secpolicytemplate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPolicyTemplateNamespace() {
        return Response
            .ok()
            .entity(Util.DoubleURLencode(Namespaces.URI_OPENTOSCA_SECURE_POLICYTEMPLATE))
            .build();
    }
}
