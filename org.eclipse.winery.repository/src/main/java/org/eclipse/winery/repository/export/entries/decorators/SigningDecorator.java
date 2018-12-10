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
package org.eclipse.winery.repository.export.entries.decorators;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;

import org.eclipse.winery.security.algorithm.SignatureAlgorithm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SigningDecorator extends CsarEntryDecorator {
    private PrivateKey key;
    private SignatureAlgorithm algorithm;
    private static final Logger LOGGER = LoggerFactory.getLogger(SigningDecorator.class);

    public SigningDecorator(SignatureAlgorithm algorithm, PrivateKey key) {
        this.key = key;
        this.algorithm = algorithm;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream result = null;
        
        try(InputStream beforeDecoration = this.toDecorate.getInputStream()) {
            final byte[] signatureAsBytes = this.algorithm.signStream(beforeDecoration, key);
            result = new ByteArrayInputStream(signatureAsBytes);
            
        } catch (SignatureException|InvalidKeyException e) {
            LOGGER.error("Unexpected security exception occurred. Reason: {}", e.getMessage());
        }
        
        return result;
    }
    
}
