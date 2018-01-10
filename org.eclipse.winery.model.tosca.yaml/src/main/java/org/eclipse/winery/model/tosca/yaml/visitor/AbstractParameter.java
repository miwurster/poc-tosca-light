/********************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.model.tosca.yaml.visitor;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractParameter<P extends AbstractParameter> implements IParameter<P> {
    private List<String> context;

    public AbstractParameter() {
        this.context = new ArrayList<>();
    }

    @Override
    public String getKey() {
        if (this.context.size() > 0) {
            return this.context.get(this.context.size() - 1);
        } else return "";
    }

    @Override
    public List<String> getContext() {
        return this.context;
    }

    @Override
    public P addContext(String key) {
        this.context.add(key);
        return self();
    }

    @Override
    public P addContext(List<String> context) {
        this.context.addAll(context);
        return self();
    }

    @Override
    public P addContext(String listName, String key) {
        this.context.add(listName);
        this.context.add(key);
        return self();
    }
}
