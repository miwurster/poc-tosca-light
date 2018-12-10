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
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Objects;

import org.eclipse.winery.security.support.SignatureAlgorithmEnum;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SignatureAlgorithmImplTest extends AbstractSecurityTestClass {
    private static final String MESSAGE = "This is an arbitrary message used for testing purposes!";
    private static final int KEY_SIZE_BITS = 1024;
    private static final String LARGE_FILE_NAME = "largeFile.bmp";
    private static final SignatureAlgorithmEnum ALGORITHM = SignatureAlgorithmEnum.RSA_SHA256;
    private static final Logger LOGGER = LoggerFactory.getLogger(SignatureAlgorithmImplTest.class);
    private SignatureAlgorithm algorithm;
    private KeyPair keyPair;

    @BeforeEach
    @Override
    public void setUp() throws NoSuchAlgorithmException, NoSuchProviderException {
        super.setUp();
        this.algorithm = new SignatureAlgorithmImpl(ALGORITHM);
        
        final KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM.getShortName(), BouncyCastleProvider.PROVIDER_NAME);
        generator.initialize(KEY_SIZE_BITS, new SecureRandom());
        this.keyPair = generator.generateKeyPair();
    }
    
    @Test
    void testStreamSigning() throws IOException, SignatureException, InvalidKeyException {
        try(InputStream plainText = IOUtils.toInputStream(MESSAGE)) {
            byte[] signature = this.algorithm.signStream(plainText, keyPair.getPrivate());
            LOGGER.info("Signature size in bytes: {}", signature.length);
            plainText.reset();
            boolean result = this.algorithm.verifyStream(signature, plainText, keyPair.getPublic());
            Assertions.assertTrue(result);
        }
    }
    
    @Test
    void testLargeFileSigning() throws SignatureException, IOException, InvalidKeyException {
        final String filePath = Objects.requireNonNull(getClass().getClassLoader().getResource(LARGE_FILE_NAME)).getPath();
        byte[] signature = this.algorithm.signFile(filePath, keyPair.getPrivate());
        boolean result = this.algorithm.verifyFile(signature, filePath, keyPair.getPublic());
        Assertions.assertTrue(result);
    }
}
