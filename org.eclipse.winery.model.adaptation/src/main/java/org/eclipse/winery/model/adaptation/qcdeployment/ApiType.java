/*******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.model.adaptation.qcdeployment;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

class ApiType {
    private @NonNull String interfaceType;
    private @NonNull String interfaceImplementationType;

    ApiType(@Nullable String interfaceType, @Nullable String interfaceImplementationType) {
        setInterfaceType(interfaceType);
        setInterfaceImplementationType(interfaceImplementationType);
    }

    @NonNull String getInterfaceType() {
        return interfaceType;
    }

    void setInterfaceType(@Nullable String interfaceType) {
        if (interfaceType != null) {
            this.interfaceType = interfaceType;
        } else {
            this.interfaceType = "";
        }
    }

    @NonNull String getInterfaceImplementationType() {
        return interfaceImplementationType;
    }

    void setInterfaceImplementationType(@Nullable String interfaceImplementationType) {
        if (interfaceImplementationType != null) {
            this.interfaceImplementationType = interfaceImplementationType;
        } else {
            this.interfaceImplementationType = "";
        }
    }
    
    @Override
    public String toString() {
        return "{InterfaceType='" + interfaceType + "', InterfaceImplementationType='" + interfaceImplementationType + "'}";
    }
}
