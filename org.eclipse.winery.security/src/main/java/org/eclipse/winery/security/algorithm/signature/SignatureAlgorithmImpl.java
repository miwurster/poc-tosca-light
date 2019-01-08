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
package org.eclipse.winery.security.algorithm.signature;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import org.eclipse.winery.security.support.enums.SignatureAlgorithmEnum;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignatureAlgorithmImpl implements SignatureAlgorithm {
    private static final Logger LOGGER = LoggerFactory.getLogger(SignatureAlgorithmImpl.class);

    /**
     * buffer with a size of 1MB
     */
    private static final int BUFFER_LENGTH = 1048576;

    private Signature signature;

    public SignatureAlgorithmImpl(String algorithmName, boolean isFullName) throws NoSuchAlgorithmException {
        try {
            String fullName = "";
            if (isFullName) {
                // e.g., SHA256withRSA
                fullName = algorithmName;
            } else {
                // e.g., RSA
                fullName = SignatureAlgorithmEnum.getDefaultOptionForAlgorithmAsString(algorithmName);
            }
            this.signature = Signature.getInstance(fullName, BouncyCastleProvider.PROVIDER_NAME);
        } catch (NoSuchProviderException e) {
            LOGGER.error("Cannot instantiate the signature algorithm. Reason: {}", e.getMessage());
        }
    }

    public SignatureAlgorithmImpl(SignatureAlgorithmEnum algorithm) throws NoSuchAlgorithmException {
        this(algorithm.getFullName(), true);
    }

    private void feedStreamToAlgorithm(InputStream stream) throws IOException, SignatureException {
        byte[] buffer = new byte[BUFFER_LENGTH];
        int bufferLength;

        while ((bufferLength = stream.read(buffer)) != -1) {
            signature.update(buffer, 0, bufferLength);
        }
    }

    @Override
    public byte[] signStream(InputStream plainText, PrivateKey key) throws IOException, SignatureException, InvalidKeyException {
        this.signature.initSign(key);
        feedStreamToAlgorithm(plainText);

        return signature.sign();
    }

    @Override
    public byte[] signFile(String filePath, PrivateKey key) throws IOException, SignatureException, InvalidKeyException {
        File file = new File(filePath);
        byte[] result;

        try (InputStream stream = new FileInputStream(file)) {
            result = this.signStream(stream, key);
        }

        return result;
    }

    @Override
    public byte[] signBytes(byte[] plainText, PrivateKey key) throws SignatureException, InvalidKeyException {
        this.signature.initSign(key);
        this.signature.update(plainText);
        
        return this.signature.sign();
    }

    @Override
    public boolean verifyStream(byte[] signatureBytes, InputStream signedPlainText, PublicKey key) throws InvalidKeyException, IOException, SignatureException {
        this.signature.initVerify(key);
        feedStreamToAlgorithm(signedPlainText);

        return this.signature.verify(signatureBytes);
    }

    @Override
    public boolean verifyFile(byte[] signatureBytes, String filePath, PublicKey key) throws InvalidKeyException, IOException, SignatureException {
        File file = new File(filePath);
        boolean result;

        try (InputStream stream = new FileInputStream(file)) {
            result = this.verifyStream(signatureBytes, stream, key);
        }

        return result;
    }

    @Override
    public boolean verifyBytes(byte[] signatureBytes, byte[] plainText, PublicKey key) throws InvalidKeyException, SignatureException {
        this.signature.initVerify(key);
        this.signature.update(plainText);

        return this.signature.verify(signatureBytes);
    }
}
