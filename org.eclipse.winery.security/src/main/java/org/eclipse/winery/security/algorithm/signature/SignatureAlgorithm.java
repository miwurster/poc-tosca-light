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
package org.eclipse.winery.security.algorithm.signature;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;

import org.eclipse.winery.security.support.enums.SignatureAlgorithmEnum;

public interface
SignatureAlgorithm {
    byte[] signStream(InputStream plainText, PrivateKey key) throws IOException, SignatureException, InvalidKeyException;

    byte[] signFile(String filePath, PrivateKey key) throws IOException, SignatureException, InvalidKeyException;
    
    byte[] signBytes(byte[] plainText, PrivateKey key) throws SignatureException, InvalidKeyException;

    boolean verifyStream(byte[] signatureBytes, InputStream signedPlainText, PublicKey key) throws InvalidKeyException, IOException, SignatureException;

    boolean verifyFile(byte[] signatureBytes, String filePath, PublicKey key) throws InvalidKeyException, IOException, SignatureException;
    
    boolean verifyBytes(byte[] signatureBytes, byte[] plainText, PublicKey key) throws InvalidKeyException, SignatureException;
    
    static SignatureAlgorithmEnum getDefaultAlgorithmForKey(Key key) throws IllegalArgumentException {

        if(key instanceof PublicKey || key instanceof PrivateKey) {
            String algorithm = key.getAlgorithm();
            return SignatureAlgorithmEnum.getDefaultOptionForAlgorithm(algorithm);
        }
        
        throw new IllegalArgumentException("PublicKey or PrivateKey expected but " + key.getClass().getName() + " was found!");
    }
}
