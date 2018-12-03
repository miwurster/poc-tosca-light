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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public enum SignatureAlgorithm {
    RSA_SHA256("RSA", DigestAlgorithm.SHA256, "SHA256withRSA", true),
    RSA_SHA384("RSA", DigestAlgorithm.SHA384, "SHA384withRSA", false),
    RSA_SHA512("RSA", DigestAlgorithm.SHA512, "SHA512withRSA", false);
    
    private String name;
    private DigestAlgorithm digestAlgorithm;
    private String fullName;
    private boolean isDefault;

    SignatureAlgorithm(String name, DigestAlgorithm digestAlgorithm, String fullName, boolean isDefault) {
        this.name = name;
        this.digestAlgorithm = digestAlgorithm;
        this.fullName = fullName;
        this.isDefault = isDefault;
    }

    public String getFullName() {
        return fullName;
    }
    
    public static Collection<String> getOptionsForAlgorithm(String algorithm) {
        if (Objects.nonNull(algorithm)) {
            Collection<String> result = new ArrayList<>();
            for (SignatureAlgorithm a : values()) {
                if (algorithm.equals(a.name))
                    result.add(a.fullName);
            }
            return result;
        }
        throw new IllegalArgumentException("Chosen signature algorithm is not supported");
    }
    
    public static String getDefaultOptionForAlgorithm(String algorithm) {
        if (Objects.nonNull(algorithm)) {
            for (SignatureAlgorithm a : values()) {
                if (algorithm.equals(a.name) && a.isDefault)
                    return a.fullName;
            }
        }
        throw new IllegalArgumentException("Chosen signature algorithm is not supported");
    }
}
