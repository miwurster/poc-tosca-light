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

package org.eclipse.winery.repository.security.csar.datatypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class KeyPairInformation {
    @JsonProperty
    private KeyEntityInformation privateKey;
    @JsonProperty
    private KeyEntityInformation publicKey;
    @JsonProperty
    private CertificateInformation certificate;

    public KeyPairInformation(KeyEntityInformation privateKey, KeyEntityInformation publicKey, CertificateInformation certificates) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.certificate = certificates;
    }

    public KeyEntityInformation getPrivateKey() {
        return privateKey;
    }

    public KeyEntityInformation getPublicKey() {
        return publicKey;
    }

    public CertificateInformation getCertificate() {
        return certificate;
    }
    
    @JsonIgnore
    public String getPEMEncodedCertificateString() {
        return "";
    }
}
