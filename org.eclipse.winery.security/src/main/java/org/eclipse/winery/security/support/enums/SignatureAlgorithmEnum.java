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

package org.eclipse.winery.security.support.enums;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public enum SignatureAlgorithmEnum {
    RSA_SHA256("RSA", DigestAlgorithmEnum.SHA256, "SHA256withRSA", true),
    RSA_SHA384("RSA", DigestAlgorithmEnum.SHA384, "SHA384withRSA", false),
    RSA_SHA512("RSA", DigestAlgorithmEnum.SHA512, "SHA512withRSA", false),
    ECDSA("EC", DigestAlgorithmEnum.SHA256, "SHA256withECDSA", true);
    
    private String family;
    private DigestAlgorithmEnum digestAlgorithm;
    private String fullName;
    private boolean isDefault;

    SignatureAlgorithmEnum(String family, DigestAlgorithmEnum digestAlgorithm, String fullName, boolean isDefault) {
        this.family = family;
        this.digestAlgorithm = digestAlgorithm;
        this.fullName = fullName;
        this.isDefault = isDefault;
    }
    
    public String getFamily() { return family; }
    
    public String getFullName() {
        return fullName;
    }
    
    public static Collection<String> getOptionsForAlgorithm(String algorithm) {
        if (Objects.nonNull(algorithm)) {
            Collection<String> result = new ArrayList<>();
            for (SignatureAlgorithmEnum a : values()) {
                if (algorithm.equals(a.family))
                    result.add(a.fullName);
            }
            return result;
        }
        throw new IllegalArgumentException("Chosen signature algorithm is not supported");
    }
    
    public static String getDefaultOptionForAlgorithmAsString(String algorithm) {
        return getDefaultOptionForAlgorithm(algorithm).fullName;
    }

    public static SignatureAlgorithmEnum getDefaultOptionForAlgorithm(String algorithmFamily) {
        if (Objects.nonNull(algorithmFamily)) {
            for (SignatureAlgorithmEnum a : values()) {
                // the following allows matching ECIES to EC
                if (algorithmFamily.startsWith(a.family) && a.isDefault)
                    return a;
            }
        }
        throw new IllegalArgumentException("Chosen signature algorithm is not supported");
    }
}
