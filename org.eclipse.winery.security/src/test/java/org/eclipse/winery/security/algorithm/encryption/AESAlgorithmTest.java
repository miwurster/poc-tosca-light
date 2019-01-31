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

import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;

class AESAlgorithmTest extends AbstractEncryptionTestingClass {
    private static final int KEY_SIZE_BITS = 256;
    private static final String ALGORITHM_NAME = "AES";
    private SecretKey key;
    
    @BeforeEach
    @Override
    public void setUp() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        super.setUp();
        algorithm = new AESAlgorithm();

        KeyGenerator keyGenerator;
        keyGenerator = KeyGenerator.getInstance(ALGORITHM_NAME, BouncyCastleProvider.PROVIDER_NAME);
        keyGenerator.init(KEY_SIZE_BITS, new SecureRandom());
        this.key = keyGenerator.generateKey();
    }

    @Override
    protected Key getEncryptionKey() {
        return key;
    }

    @Override
    protected Key getDecryptionKey() {
        return key;
    }
}
