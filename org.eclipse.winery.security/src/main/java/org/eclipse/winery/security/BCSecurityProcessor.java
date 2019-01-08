/********************************************************************************
 * Copyright (c) 2018-2019 Contributors to the Eclipse Foundation
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
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.eclipse.winery.security.algorithm.encryption.EncryptionAlgorithm;
import org.eclipse.winery.security.algorithm.signature.SignatureAlgorithm;
import org.eclipse.winery.security.algorithm.signature.SignatureAlgorithmImpl;
import org.eclipse.winery.security.datatypes.DistinguishedName;
import org.eclipse.winery.security.exceptions.GenericSecurityProcessorException;
import org.eclipse.winery.security.support.CertificateHelper;
import org.eclipse.winery.security.support.DigestHelper;
import org.eclipse.winery.security.support.KeyGenerationHelper;
import org.eclipse.winery.security.support.enums.AsymmetricEncryptionAlgorithmEnum;
import org.eclipse.winery.security.support.enums.DigestAlgorithmEnum;
import org.eclipse.winery.security.support.enums.SignatureAlgorithmEnum;
import org.eclipse.winery.security.support.enums.SymmetricEncryptionAlgorithmEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the SecurityProcessor interface that uses the BouncyCastle security provider.
 */
class BCSecurityProcessor implements SecurityProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BCSecurityProcessor.class);
    private EncryptionAlgorithm symmetricEncryption;
    private EncryptionAlgorithm asymmetricEncryption;
    private Map<SignatureAlgorithmEnum, SignatureAlgorithm> signatureAlgorithms;

    BCSecurityProcessor(EncryptionAlgorithm symmetricEncryption, EncryptionAlgorithm asymmetricEncryption) {
        this.symmetricEncryption = symmetricEncryption;
        this.asymmetricEncryption = asymmetricEncryption;
        this.signatureAlgorithms = new HashMap<>();
    }

    @Override
    public SecretKey generateSecretKey(SymmetricEncryptionAlgorithmEnum algorithm, int keySize) throws GenericSecurityProcessorException {
        return KeyGenerationHelper.generateSecretKey(algorithm.getName(), keySize);
    }

    @Override
    public KeyPair generateKeyPair(AsymmetricEncryptionAlgorithmEnum algorithm, int keySize) throws GenericSecurityProcessorException {
        // keys for algorithms with parameter specifications set (e.g., ECIES) is generated differently 
        if (algorithm.getAlgorithmParameterSpec() != null)
            return KeyGenerationHelper.generateKeyPair(algorithm.getName(), algorithm.getAlgorithmParameterSpec());
        return KeyGenerationHelper.generateKeyPair(algorithm.getName(), algorithm.getKeySizeInBits());
    }

    @Override
    public SecretKey getSecretKeyFromInputStream(SymmetricEncryptionAlgorithmEnum algorithm, InputStream secretKeyInputStream) throws GenericSecurityProcessorException {
        return KeyGenerationHelper.getSecretKeyFromInputStream(algorithm.getName(), secretKeyInputStream);
    }

    @Override
    public PrivateKey getPKCS8PrivateKeyFromInputStream(AsymmetricEncryptionAlgorithmEnum algorithm, InputStream privateKeyInputStream) throws GenericSecurityProcessorException {
        return KeyGenerationHelper.getPKCS8PrivateKeyFromInputStream(algorithm.getName(), privateKeyInputStream);
    }

    @Override
    public PublicKey getX509EncodedPublicKeyFromInputStream(AsymmetricEncryptionAlgorithmEnum algorithm, InputStream publicKeyInputStream) throws GenericSecurityProcessorException {
        return KeyGenerationHelper.getX509EncodedPublicKeyFromInputStream(algorithm.getName(), publicKeyInputStream);
    }

    @Override
    public Certificate generateSelfSignedX509Certificate(KeyPair keypair, DistinguishedName distinguishedName) throws GenericSecurityProcessorException {
        return CertificateHelper.generateSelfSignedX509Certificate(keypair, distinguishedName);
    }

    @Override
    public Certificate[] getX509Certificates(InputStream certInputStream) throws GenericSecurityProcessorException {
        return CertificateHelper.getX509Certificates(certInputStream);
    }

    @Override
    public EncryptionAlgorithm getSymmetricEncryptionAlgorithm() {
        return this.symmetricEncryption;
    }

    @Override
    public EncryptionAlgorithm getAsymmetricEncryptionAlgorithm() {
        return this.asymmetricEncryption;
    }

    @Override
    public SignatureAlgorithm getSignatureAlgorithm(SignatureAlgorithmEnum algorithm) throws NoSuchAlgorithmException {
        if (!this.signatureAlgorithms.containsKey(algorithm)) {
            this.signatureAlgorithms.put(algorithm, new SignatureAlgorithmImpl(algorithm));
        }
        
        return this.signatureAlgorithms.get(algorithm);
    }

    @Override
    public String getChecksumForFile(String absolutePath, DigestAlgorithmEnum algorithm) {
        return DigestHelper.getChecksumForFile(absolutePath, algorithm.getName());
    }

    @Override
    public String getChecksumForFile(File file, DigestAlgorithmEnum algorithm) throws IOException, NoSuchAlgorithmException {
        return DigestHelper.getChecksumForFile(file, algorithm.getName());
    }

    @Override
    public String getChecksumForString(String str, DigestAlgorithmEnum algorithm) throws IOException, NoSuchAlgorithmException {
        return DigestHelper.getChecksumForString(str, algorithm.getName());
    }

    @Override
    public String getChecksum(InputStream content, DigestAlgorithmEnum algorithm) throws IOException, NoSuchAlgorithmException {
        return DigestHelper.getChecksum(content, algorithm.getName());
    }

    @Override
    public byte[] getChecksumAsBytes(InputStream content, DigestAlgorithmEnum algorithm) throws NoSuchAlgorithmException, IOException {
        return DigestHelper.getChecksumAsBytes(content, algorithm.getName());
    }
}
