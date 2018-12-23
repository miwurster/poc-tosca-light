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

package org.eclipse.winery.security;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

import javax.crypto.SecretKey;

import org.eclipse.winery.security.algorithm.encryption.EncryptionAlgorithm;
import org.eclipse.winery.security.algorithm.signature.SignatureAlgorithm;
import org.eclipse.winery.security.datatypes.DistinguishedName;
import org.eclipse.winery.security.exceptions.GenericSecurityProcessorException;
import org.eclipse.winery.security.support.AsymmetricEncryptionAlgorithmEnum;
import org.eclipse.winery.security.support.DigestAlgorithmEnum;
import org.eclipse.winery.security.support.SymmetricEncryptionAlgorithmEnum;

/**
 * Provides access to cryptographic primitives (symmetric and asymmetric encryption, key generation and certificate 
 * generation, signing and verification of signatures, and calculation of message digests.
 * Use the SecurityProcessorFactory to get an instance of this type.
 */
public interface SecurityProcessor {

    SecretKey generateSecretKey(SymmetricEncryptionAlgorithmEnum algorithm, int keySize) throws GenericSecurityProcessorException;

    KeyPair generateKeyPair(AsymmetricEncryptionAlgorithmEnum algorithm, int keySize) throws GenericSecurityProcessorException;

    SecretKey getSecretKeyFromInputStream(SymmetricEncryptionAlgorithmEnum algorithm, InputStream secretKeyInputStream) throws GenericSecurityProcessorException;

    PrivateKey getPKCS8PrivateKeyFromInputStream(AsymmetricEncryptionAlgorithmEnum algorithm, InputStream privateKeyInputStream) throws GenericSecurityProcessorException;

    PublicKey getX509EncodedPublicKeyFromInputStream(AsymmetricEncryptionAlgorithmEnum algorithm, InputStream publicKeyInputStream) throws GenericSecurityProcessorException;

    Certificate[] getX509Certificates(InputStream certInputStream) throws GenericSecurityProcessorException;

    Certificate generateSelfSignedX509Certificate(KeyPair keypair, DistinguishedName distinguishedName) throws GenericSecurityProcessorException;

    EncryptionAlgorithm getSymmetricEncryptionAlgorithm();
    
    EncryptionAlgorithm getAsymmetricEncryptionAlgorithm();
    
    SignatureAlgorithm getSignatureAlgorithm();

    String getChecksumForFile(String absolutePath, DigestAlgorithmEnum algorithm);

    String getChecksumForFile(File file, DigestAlgorithmEnum algorithm) throws IOException, NoSuchAlgorithmException;

    String getChecksumForString(String str, DigestAlgorithmEnum algorithm) throws IOException, NoSuchAlgorithmException;

    String getChecksum(InputStream content, DigestAlgorithmEnum algorithm) throws IOException, NoSuchAlgorithmException;

    byte[] getChecksumAsBytes(InputStream content, DigestAlgorithmEnum algorithm) throws NoSuchAlgorithmException, IOException;
}
