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

import java.security.NoSuchAlgorithmException;

import org.eclipse.winery.security.algorithm.encryption.AESAlgorithm;
import org.eclipse.winery.security.algorithm.encryption.ECIESAlgorithm;
import org.eclipse.winery.security.algorithm.signature.SignatureAlgorithmImpl;
import org.eclipse.winery.security.support.SignatureAlgorithmEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityProcessorFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityProcessorFactory.class);
    private static BCSecurityProcessor bcSecurityProcessor;

    private static BCSecurityProcessor getDefaultBcSecurityProcessorInstance() {
        try {
            if (bcSecurityProcessor == null) {
                bcSecurityProcessor = new BCSecurityProcessor(new AESAlgorithm(), new ECIESAlgorithm(), new SignatureAlgorithmImpl(SignatureAlgorithmEnum.RSA_SHA256));
            }
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Cannot instantiate SecurityProcessor", e);
        }
        
        return bcSecurityProcessor;
    }
    
    public static SecurityProcessor getDefaultSecurityProcessor() {
        return  getDefaultBcSecurityProcessorInstance();
    }
}
