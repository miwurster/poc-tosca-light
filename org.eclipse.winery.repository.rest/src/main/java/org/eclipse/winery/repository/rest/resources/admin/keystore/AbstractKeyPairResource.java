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
package org.eclipse.winery.repository.rest.resources.admin.keystore;

import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.Objects;

import org.eclipse.winery.repository.security.csar.SecureCSARConstants;
import org.eclipse.winery.security.KeystoreManager;
import org.eclipse.winery.security.SecurityProcessor;
import org.eclipse.winery.security.exceptions.GenericKeystoreManagerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractKeyPairResource extends AbstractKeystoreEntityResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractKeyPairResource.class);

    public AbstractKeyPairResource(KeystoreManager keystoreManager, SecurityProcessor securityProcessor) {
        super(keystoreManager, securityProcessor);
    }

    protected String renameKeyPair(String oldName, String newName) {
        try {
            // check if the master alias is used before
            KeyPair keyPair = this.keystoreManager.loadKeyPair(oldName);
            Certificate oldMasterCertificate = this.keystoreManager.loadCertificate(oldName);
            String newAlias = newName;

            if (Objects.isNull(newName)) {
                newAlias = this.generateUniqueAlias(keyPair.getPublic());
            }

            this.keystoreManager.storeKeyPair(newAlias, keyPair.getPrivate(), oldMasterCertificate);
            this.keystoreManager.deleteKeystoreEntry(oldName);

            return newAlias;
        } catch (GenericKeystoreManagerException e) {
            LOGGER.info("No keypair has the name: " + oldName);
        } catch (IOException | NoSuchAlgorithmException e) {
            LOGGER.error("An unexpected error happened. Reason: " + e.getMessage(), e);
        }

        return null;
    }

    protected String renameOldMaster() {
        return renameKeyPair(SecureCSARConstants.MASTER_SIGNING_KEYNAME, null);
    }
}
