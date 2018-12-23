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

package org.eclipse.winery.security.support;

public enum DigestAlgorithmEnum {
    SHA256("SHA-256"),
    SHA384("SHA-384"),
    SHA512("SHA-512");

    private String name;
    
    DigestAlgorithmEnum(String name) {
        this.name = name;
    }
    
    public String getName() { return this.name; }
    
    public static DigestAlgorithmEnum findByName(String name) throws IllegalArgumentException {
        for(DigestAlgorithmEnum current : values()) {
            if (current.name.equalsIgnoreCase(name))
                return current;
        }
        
        throw new IllegalArgumentException("The specified digest algorithm is not supported: " + name);
    }
}
