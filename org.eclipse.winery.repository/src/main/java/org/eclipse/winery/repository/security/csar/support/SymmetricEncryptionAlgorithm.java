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

package org.eclipse.winery.repository.security.csar.support;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum SymmetricEncryptionAlgorithm {
    AES256("AES", 256),
    AES512("AES", 512),
    DES256("DES", 256),
    DES512("DES", 512);    

    private String name;
    private int keySizeInBits;

    SymmetricEncryptionAlgorithm(String algorithm, int keySizeInBits) {
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
