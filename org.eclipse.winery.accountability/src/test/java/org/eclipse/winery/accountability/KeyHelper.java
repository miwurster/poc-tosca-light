/********************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.accountability;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.winery.security.support.DigestHelper;
import org.eclipse.winery.security.support.enums.AsymmetricEncryptionAlgorithmEnum;
import org.eclipse.winery.security.support.enums.SymmetricEncryptionAlgorithmEnum;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class KeyHelper {
    public static SecretKey convertStringToSecretKey(String passphrase, SymmetricEncryptionAlgorithmEnum algorithm) throws IOException, NoSuchAlgorithmException {
        if (algorithm.getkeySizeInBits() > 512)
            throw new IllegalArgumentException("key size not supported by this test!");

        byte[] hash = DigestHelper.getChecksumAsBytes(IOUtils.toInputStream(passphrase, Charset.defaultCharset()), "SHA-512");
        byte[] keyBytes = new byte[algorithm.getkeySizeInBits() / 8];
        System.arraycopy(hash, 0, keyBytes, 0, keyBytes.length);

        return new SecretKeySpec(keyBytes, algorithm.getName());
    }
    
    public static KeyPair generateECIESKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        KeyPairGenerator g = KeyPairGenerator.getInstance(AsymmetricEncryptionAlgorithmEnum.ECIES_secp256k1.getName(), BouncyCastleProvider.PROVIDER_NAME);
        g.initialize(AsymmetricEncryptionAlgorithmEnum.ECIES_secp256k1.getAlgorithmParameterSpec(), new SecureRandom());
        return g.generateKeyPair();
    }

    public static boolean verifyKey(SecretKey key, String passphrase) throws IOException, NoSuchAlgorithmException {
        byte[] hash = DigestHelper.getChecksumAsBytes(IOUtils.toInputStream(passphrase, Charset.defaultCharset()), "SHA-512");
        byte[] keyBytes = key.getEncoded();

        for (int i = 0; i < keyBytes.length; i++) {
            if (hash[i] != keyBytes[i]) {
                return false;
            }
        }

        return true;
    }
    
}
