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

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum AsymmetricEncryptionAlgorithmEnum {
    RSA1024("RSA", 1024),
    RSA2048("RSA", 2048),
    ECIES256("ECIES", 256);

    private String name;
    private int keySizeInBits;
    
    AsymmetricEncryptionAlgorithmEnum(String algorithm, int keySizeInBits) {
        this.name = algorithm;
        this.keySizeInBits = keySizeInBits;
    }

    public String getName() {
        return this.name;
    }

    public int getkeySizeInBits() {
        return this.keySizeInBits;
    }
}
