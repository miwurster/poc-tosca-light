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

package org.eclipse.winery.security.algorithm.encryption;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import org.eclipse.winery.security.support.enums.AsymmetricEncryptionAlgorithmEnum;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;

class ECIESAlgorithmTest extends AbstractEncryptionTestingClass {
    private static final String ALGORITHM_NAME = "ECIES";
    private KeyPair keyPair;
    
    @BeforeEach
    @Override
    public void setUp() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, IOException {
        super.setUp();
        algorithm = new ECIESAlgorithm();

        KeyPairGenerator    g = KeyPairGenerator.getInstance(ALGORITHM_NAME, BouncyCastleProvider.PROVIDER_NAME);
        g.initialize(AsymmetricEncryptionAlgorithmEnum.ECIES_secp256k1.getAlgorithmParameterSpec(), new SecureRandom());
        this.keyPair = g.generateKeyPair();
    }

    @Override
    protected Key getEncryptionKey() {
        return keyPair.getPublic();
    }

    @Override
    protected Key getDecryptionKey() {
        return keyPair.getPrivate();
    }
}
