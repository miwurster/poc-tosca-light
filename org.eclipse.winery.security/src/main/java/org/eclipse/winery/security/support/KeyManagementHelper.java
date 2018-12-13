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
package org.eclipse.winery.security.support;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.winery.security.exceptions.GenericSecurityProcessorException;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyManagementHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyManagementHelper.class);
    
    public static SecretKey generateSecretKey(String algorithm, int keySize) throws GenericSecurityProcessorException {
        try {
            KeyGenerator keyGenerator;
            keyGenerator = KeyGenerator.getInstance(algorithm, BouncyCastleProvider.PROVIDER_NAME);
            keyGenerator.init(keySize, new SecureRandom());
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            LOGGER.error("Error generating a secret key", e);
            throw new GenericSecurityProcessorException("Could not generate the secret key with given properties", e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Requested combination of the algorithm and key size is not supported", e);
            throw new GenericSecurityProcessorException("Requested combination of the algorithm and key size is not supported", e);
        }
    }
    
    
    public static KeyPair generateKeyPair(String algorithm, int keySize) throws GenericSecurityProcessorException {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm, BouncyCastleProvider.PROVIDER_NAME);
            keyPairGenerator.initialize(keySize, new SecureRandom());
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            LOGGER.error("Error generating a key pair", e);
            throw new GenericSecurityProcessorException("Could not generate the secret key with given properties", e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Requested combination of the algorithm and key size is not supported", e);
            throw new GenericSecurityProcessorException("Requested combination of the algorithm and key size is not supported", e);
        }
    }

    public static KeyPair generateKeyPair(String algorithm, AlgorithmParameterSpec algorithmParameterSpec) throws GenericSecurityProcessorException {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm, BouncyCastleProvider.PROVIDER_NAME);
            keyPairGenerator.initialize(algorithmParameterSpec, new SecureRandom());
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            LOGGER.error("Error generating a key pair", e);
            throw new GenericSecurityProcessorException("Could not generate the secret key with given properties", e);
        } catch (InvalidAlgorithmParameterException e) {
            String msg = "The supplied algorithm parameter is invalid";
            LOGGER.error(msg, e);
            throw new GenericSecurityProcessorException(msg, e);
        }
    }
    
    public static SecretKey getSecretKeyFromInputStream(String algorithm, InputStream secretKeyInputStream) throws GenericSecurityProcessorException {
        try {
            byte[] key;
            key = IOUtils.toByteArray(secretKeyInputStream);
            return new SecretKeySpec(key, 0, key.length, algorithm);
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.error("Error processing the provided secret key", e);
            throw new GenericSecurityProcessorException("Error processing the provided secret key", e);
        }
    }
    
    public static PrivateKey getPKCS8PrivateKeyFromInputStream(String algorithm, InputStream privateKeyInputStream) throws GenericSecurityProcessorException {
        try {
            byte[] privateKeyByteArray;
            privateKeyByteArray = IOUtils.toByteArray(privateKeyInputStream);
            KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyByteArray);
            return keyFactory.generatePrivate(privateKeySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | IOException e) {
            LOGGER.error("Error processing the provided private key", e);
            throw new GenericSecurityProcessorException("Error processing the provided private key", e);
        }
    }

    public static PublicKey getX509EncodedPublicKeyFromInputStream(String algorithm, InputStream publicKeyInputStream) throws GenericSecurityProcessorException {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
            byte[] publicKeyByteArray = new byte[0];
            publicKeyByteArray = IOUtils.toByteArray(publicKeyInputStream);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyByteArray);
            return keyFactory.generatePublic(publicKeySpec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            LOGGER.error("Error processing the provided public key", e);
            throw new GenericSecurityProcessorException("Error processing the provided public key", e);
        }
    }
}
