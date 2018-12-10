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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AESAlgorithm implements SymmetricEncryptionAlgorithm {

    /**
     * The name of the encryption scheme
     */
    public static final String SCHEME = "AES/CBC/PKCS5Padding";

    /**
     * The name of method used to implement a pseudo-random number generator
     */
    private static final String PRNG_NAME = "SHA1PRNG";
    private static final Logger LOGGER = LoggerFactory.getLogger(AESAlgorithm.class);

    /**
     * Generates an initialization vector using a secure PRNG and adds it to the beginning of the ciphertext (CBC mode)
     *
     * @param cipher the encryption algorithm
     * @return the initialization vector
     * @throws NoSuchAlgorithmException if the secure PRNG is not found (should not happen!)
     */
    private IvParameterSpec generateRandomIV(Cipher cipher) throws NoSuchAlgorithmException {
        final int blockSize = cipher.getBlockSize();

        // generate random IV using block size
        final byte[] ivData = new byte[blockSize];
        final SecureRandom rnd;
        rnd = SecureRandom.getInstance(PRNG_NAME);
        rnd.nextBytes(ivData);

        return new IvParameterSpec(ivData);
    }

    /**
     * Retrieves the initialization vector from the beginning of the given ciphertext (CBC mode)
     *
     * @param cipher     the encryption algorithm
     * @param cipherText ciphertext produced with the scheme CBC
     * @return the retrieved initialization vector
     * @throws IOException if reading from the input stream of the ciphertext fails.
     */
    private IvParameterSpec retrieveIVFromCipherText(Cipher cipher, InputStream cipherText) throws IOException {
        byte[] buffer = new byte[cipher.getBlockSize()];
        IOUtils.read(cipherText, buffer, 0, buffer.length);

        return new IvParameterSpec(buffer);
    }

    @Override
    public InputStream encryptStream(SecretKey key, InputStream plainText) {
        SequenceInputStream result = null;
        try {
            final Cipher cipher = Cipher.getInstance(SCHEME);
            final IvParameterSpec iv = generateRandomIV(cipher);
            final ByteArrayInputStream ivStream = new ByteArrayInputStream(iv.getIV());
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            final CipherInputStream cipherStream = new CipherInputStream(plainText, cipher);
            // When the following stream is closed, both the ivStream and the cipherStream will also be closed!
            result = new SequenceInputStream(ivStream, cipherStream);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            // We should never get into here!
            LOGGER.error("Unexpected cryptographic exception occurred: {}", e.getMessage());
        }

        return result;
    }

    @Override
    public byte[] encryptBytes(SecretKey key, byte[] plainText) {
        byte[] result = null;

        try (ByteArrayInputStream plainTextStream = new ByteArrayInputStream(plainText)) {
            try (InputStream cipherStream = this.encryptStream(key, plainTextStream)) {
                result = IOUtils.toByteArray(cipherStream);
            }
        } catch (IOException e) {
            LOGGER.error("Unexpected IO exception occurred: {}", e.getMessage());
        }

        return result;
    }

    @Override
    public InputStream decryptStream(SecretKey key, InputStream cipherText) throws IOException {
        CipherInputStream cipherStream = null;
        try {
            final Cipher cipher = Cipher.getInstance(SCHEME);
            final IvParameterSpec iv = retrieveIVFromCipherText(cipher, cipherText);
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            cipherStream = new CipherInputStream(cipherText, cipher);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            // We should never get into here!
            LOGGER.error("Unexpected cryptographic exception occurred: {}", e.getMessage());
        }

        return cipherStream;
    }

    @Override
    public byte[] decryptBytes(SecretKey key, byte[] cipherTextBytes) {
        byte[] result = null;

        try (ByteArrayInputStream cipherTextStream = new ByteArrayInputStream(cipherTextBytes)) {
            try (InputStream plainTextStream = this.decryptStream(key, cipherTextStream)) {
                result = IOUtils.toByteArray(plainTextStream);
            }
        } catch (IOException e) {
            LOGGER.error("Unexpected IO exception occurred: {}", e.getMessage());
        }

        return result;
    }
}
