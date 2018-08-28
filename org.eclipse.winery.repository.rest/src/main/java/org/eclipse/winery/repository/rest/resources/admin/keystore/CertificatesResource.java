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

import java.io.InputStream;
import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.winery.repository.security.csar.KeystoreManager;
import org.eclipse.winery.repository.security.csar.SecurityProcessor;
import org.eclipse.winery.repository.security.csar.datatypes.CertificateInformation;
import org.eclipse.winery.repository.security.csar.exceptions.GenericKeystoreManagerException;

import com.sun.jersey.multipart.FormDataParam;

public class CertificatesResource extends AbstractKeystoreEntityResource {
    public CertificatesResource(KeystoreManager keystoreManager, SecurityProcessor securityProcessor) {
        super(keystoreManager, securityProcessor);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<CertificateInformation> getCertificatesList() {
        try {
            return this.keystoreManager.getCertificates();
        } catch (GenericKeystoreManagerException e) {
            throw new WebApplicationException(
                Response.serverError()
                    .entity(e.getMessage())
                    .type(MediaType.TEXT_PLAIN)
                    .build()
            );
        }
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadExistingCertificate(@FormDataParam("alias") String alias,
                                              @FormDataParam("certificate") InputStream certificate) {
        // TODO
        return Response.noContent().build();
    }

    @GET
    @Path("{alias}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCertificateInfo(@PathParam("alias") String alias) {
        // TODO
        return Response.noContent().build();
    }
}
