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

package org.eclipse.winery.security.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AESAlgorithmTest extends AbstractSecurityTestClass {
    private static final String MESSAGE = "This is an arbitrary message used for testing purposes!";
    private static final int KEY_SIZE_BITS = 256;
    private static final String ALGORITHM_NAME = "AES";
    private AESAlgorithm algorithm;
    private SecretKey key;

    
    @BeforeEach
    @Override
    public void setUp() throws NoSuchAlgorithmException, NoSuchProviderException {
        super.setUp();
        algorithm = new AESAlgorithm();

        KeyGenerator keyGenerator;
        keyGenerator = KeyGenerator.getInstance(ALGORITHM_NAME, BouncyCastleProvider.PROVIDER_NAME);
        keyGenerator.init(KEY_SIZE_BITS, new SecureRandom());
        this.key = keyGenerator.generateKey();
    }
    
    @Test
    void encryptDecryptStream() throws IOException {
        try(InputStream originalPlainTextStream = IOUtils.toInputStream(MESSAGE)) {
            try(InputStream cipherTextStream = this.algorithm.encryptStream(key, originalPlainTextStream)) {
                try(InputStream decryptedPlainTextStream = this.algorithm.decryptStream(key, cipherTextStream)) {
                    decryptedPlainTextStream.reset();
                    final String resultingMessage = IOUtils.toString(decryptedPlainTextStream);
                    Assertions.assertEquals(MESSAGE, resultingMessage);
                }
            }
        }
    }
    
    @Test
    void encryptDecryptByteArray() throws IOException {
        byte[] originalPlainTextBytes = MESSAGE.getBytes();
        byte[] encryptedBytes = this.algorithm.encryptBytes(this.key, originalPlainTextBytes);
        byte[] decryptedBytes = this.algorithm.decryptBytes(this.key, encryptedBytes);
        String result = IOUtils.toString(decryptedBytes, Charsets.UTF_8.name());
        Assertions.assertEquals(MESSAGE, result);
    }
    
}
