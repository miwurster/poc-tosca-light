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

package org.eclipse.winery.repository.security.csar;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

import javax.crypto.SecretKey;

import org.eclipse.winery.repository.security.csar.datatypes.DistinguishedName;
import org.eclipse.winery.repository.security.csar.exceptions.GenericSecurityProcessorException;

public interface SecurityProcessor {

    Key generateSecretKey(String algorithm, int keySize) throws GenericSecurityProcessorException;

    KeyPair generateKeyPair(String algorithm, int keySize) throws GenericSecurityProcessorException;

    Certificate generateSelfSignedX509Certificate(KeyPair keypair, DistinguishedName distinguishedName) throws GenericSecurityProcessorException;

    SecretKey getSecretKeyFromInputStream(String algorithm, InputStream secretKeyInputStream) throws GenericSecurityProcessorException;

    PrivateKey getPKCS8PrivateKeyFromInputStream(String algorithm, InputStream privateKeyInputStream) throws GenericSecurityProcessorException;

    PublicKey getX509EncodedPublicKeyFromInputStream(String algorithm, InputStream publicKeyInputStream) throws GenericSecurityProcessorException;

    Certificate[] getX509Certificates(InputStream certInputStream) throws GenericSecurityProcessorException;

    byte[] encryptBytes(Key k, byte[] sequence) throws GenericSecurityProcessorException;

    byte[] decryptBytes(Key k, byte[] sequence) throws GenericSecurityProcessorException;

    String calculateDigest(String str, String digestAlgorithm) throws GenericSecurityProcessorException;

    String calculateDigest(byte[] bytes, String digestAlgorithm) throws GenericSecurityProcessorException;

    byte[] signText(Key privateKey, String text) throws GenericSecurityProcessorException;

    byte[] signBytes(Key privateKey, byte[] text) throws GenericSecurityProcessorException;

    boolean verifyText(Certificate cert, String text, byte[] signature) throws GenericSecurityProcessorException;

    boolean verifyBytes(Certificate cert, byte[] text, byte[] signature) throws GenericSecurityProcessorException;
}
