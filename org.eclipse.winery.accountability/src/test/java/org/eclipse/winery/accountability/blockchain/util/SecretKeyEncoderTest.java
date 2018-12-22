package org.eclipse.winery.accountability.blockchain.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.winery.common.HashingUtil;
import org.eclipse.winery.security.support.SymmetricEncryptionAlgorithmEnum;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/*******************************************************************************
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
class SecretKeyEncoderTest {
    private static final String KEY_1_PHREASE = "Winery is cool!";
    private static final SymmetricEncryptionAlgorithmEnum KEY_1_ALGO = SymmetricEncryptionAlgorithmEnum.AES512;
    private static final String KEY_2_PHREASE = "Winery is not cool!";
    private static final SymmetricEncryptionAlgorithmEnum KEY_2_ALGO = SymmetricEncryptionAlgorithmEnum.AES256;
    private static final String KEY_3_PHREASE = "Weihnachtsfeier können langweilig sein!";
    private static final SymmetricEncryptionAlgorithmEnum KEY_3_ALGO = SymmetricEncryptionAlgorithmEnum.AES512;

    @Test
    public void testEncodeDecode() throws IOException, NoSuchAlgorithmException {
        SecretKey[] keys = {
            convertStringToSecretKey(KEY_1_PHREASE, KEY_1_ALGO),
            convertStringToSecretKey(KEY_2_PHREASE, KEY_2_ALGO),
            convertStringToSecretKey(KEY_3_PHREASE, KEY_3_ALGO)
        };
        byte[] encoded = SecretKeyEncoder.encode(keys);
        Assertions.assertEquals(3 + (KEY_1_ALGO.getkeySizeInBits() + KEY_2_ALGO.getkeySizeInBits() + KEY_3_ALGO.getkeySizeInBits()) / 8
            , encoded.length);
        SecretKey[] decoded = SecretKeyEncoder.decode(encoded);
        Assertions.assertEquals(3, decoded.length);
        Assertions.assertTrue(verifyKey(decoded[0], KEY_1_PHREASE));
        Assertions.assertTrue(verifyKey(decoded[1], KEY_2_PHREASE));
        Assertions.assertTrue(verifyKey(decoded[2], KEY_3_PHREASE));
    }

    private SecretKey convertStringToSecretKey(String passphrase, SymmetricEncryptionAlgorithmEnum algorithm) throws IOException, NoSuchAlgorithmException {
        if (algorithm.getkeySizeInBits() > 512)
            throw new IllegalArgumentException("key size not supported by this test!");

        byte[] hash = HashingUtil.getChecksumAsBytes(IOUtils.toInputStream(passphrase, Charset.defaultCharset()), "SHA-512");
        byte[] keyBytes = new byte[algorithm.getkeySizeInBits() / 8];
        System.arraycopy(hash, 0, keyBytes, 0, keyBytes.length);

        return new SecretKeySpec(keyBytes, algorithm.getName());
    }

    private boolean verifyKey(SecretKey key, String passphrase) throws IOException, NoSuchAlgorithmException {
        byte[] hash = HashingUtil.getChecksumAsBytes(IOUtils.toInputStream(passphrase, Charset.defaultCharset()), "SHA-512");
        byte[] keyBytes = key.getEncoded();

        for (int i = 0; i < keyBytes.length; i++) {
            if (hash[i] != keyBytes[i]) {
                return false;
            }
        }

        return true;
    }
}
