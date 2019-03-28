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
package org.eclipse.winery.repository.security.csar;

import java.io.Serializable;
import java.util.ArrayList;

public class KeyAssignments implements Serializable {
    private String keyAlias;
    private ArrayList<String> keyGivers;
    private ArrayList<String> keyTakers;

    public KeyAssignments() {
    }

    public void addGiver(String address) {
        if (this.keyGivers == null) {
            this.keyGivers = new ArrayList<>();
        }

        if (!this.keyGivers.contains(address)) {
            this.keyGivers.add(address);
        }
    }

    public void addTaker(String address) {
        if (this.keyTakers == null) {
            this.keyTakers = new ArrayList<>();
        }

        if (!this.keyTakers.contains(address)) {
            this.keyTakers.add(address);
        }
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public ArrayList<String> getKeyGivers() {
        return keyGivers;
    }

    public void setKeyGivers(ArrayList<String> keyGivers) {
        this.keyGivers = keyGivers;
    }

    public ArrayList<String> getKeyTakers() {
        return keyTakers;
    }

    public void setKeyTakers(ArrayList<String> keyTakers) {
        this.keyTakers = keyTakers;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof KeyAssignments) {
            return ((KeyAssignments) other).keyAlias.equals(this.keyAlias);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.keyAlias.hashCode();
    }
}
