/*******************************************************************************
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

package org.eclipse.winery.repository.rest.resources.servicecompositionmodels;

import javax.ws.rs.core.Response;

import org.eclipse.winery.common.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.model.tosca.TExtensibleElements;
import org.eclipse.winery.model.tosca.TServiceCompositionModel;
import org.eclipse.winery.repository.rest.RestUtils;
import org.eclipse.winery.repository.rest.resources._support.AbstractComponentInstanceResource;
import org.eclipse.winery.repository.rest.resources._support.IHasName;

public class ServiceCompositionModelResource extends AbstractComponentInstanceResource implements IHasName {
    
    /**
     * Instantiates the resource. Assumes that the resource should exist (assured by the caller)
     * <p>
     * The caller should <em>not</em> create the resource by other ways. E.g., by instantiating this resource and then
     * adding data.
     *
     * @param id
     */
    public ServiceCompositionModelResource(DefinitionsChildId id) { super(id); }

    @Override
    protected TExtensibleElements createNewElement() { return new TServiceCompositionModel(); }

    @Override
    public String getName() {
        String name = this.getServiceCompositionModel().getName();
        if (name == null) {
            // place default
            name = this.getId().getXmlId().getDecoded();
        }
        return name;
    }

    @Override
    public Response setName(String name) {
        this.getServiceCompositionModel().setName(name);
        return RestUtils.persist(this);
    }

    public TServiceCompositionModel getServiceCompositionModel() { return (TServiceCompositionModel) this.getElement(); }
}
