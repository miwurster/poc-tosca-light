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

package org.eclipse.winery.accountability;

import java.util.Objects;
import java.util.Properties;

import org.eclipse.winery.accountability.blockchain.BlockchainAccess;
import org.eclipse.winery.accountability.blockchain.BlockchainFactory;
import org.eclipse.winery.accountability.exceptions.AccountabilityException;
import org.eclipse.winery.accountability.exceptions.BlockchainException;
import org.eclipse.winery.accountability.storage.ImmutableStorageProvider;
import org.eclipse.winery.accountability.storage.ImmutableStorageProviderFactory;

import de.danielbechler.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountabilityManagerFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountabilityManagerFactory.class);
    private static AccountabilityManager accountabilityManager;
    private static Properties activeProperties;

    public static AccountabilityManager getAccountabilityManager(Properties accountabilityConfiguration) throws AccountabilityException {
        boolean requiresRecreation = false;
        // if any of the relevant configurations is changed, we need a new instance!
        if (activeProperties != null) {
            requiresRecreation = checkIfChanged(accountabilityConfiguration);
        }

        if (Objects.isNull(accountabilityManager) || requiresRecreation) {
            BlockchainFactory.reset();
            ImmutableStorageProviderFactory.reset();

            // if there is an older accountability manager, we should shut it down
            if (!Objects.isNull(accountabilityManager)) {
                accountabilityManager.close();
            }

            try {
                BlockchainAccess blockchain = BlockchainFactory.getBlockchainAccess(BlockchainFactory.AvailableBlockchains.ETHEREUM, accountabilityConfiguration);
                ImmutableStorageProvider storageProvider = ImmutableStorageProviderFactory.getStorageProvider(ImmutableStorageProviderFactory.AvailableImmutableStorages.SWARM, accountabilityConfiguration);
                accountabilityManager = new AccountabilityManagerImpl(blockchain, storageProvider);
                copyProperties(accountabilityConfiguration);
            } catch (BlockchainException e) {
                String msg = "Could not instantiate accountability layer: " + e.getMessage();
                LOGGER.error(msg, e);
                throw new AccountabilityException(msg, e);
            }
        }

        return accountabilityManager;
    }

    private static boolean checkIfChanged(Properties accountabilityConfiguration) {
        return isPropChanged(accountabilityConfiguration, "geth-url", true) ||
            isPropChanged(accountabilityConfiguration, "swarm-gateway-url", false) ||
            isPropChanged(accountabilityConfiguration, "ethereum-credentials-file-path", false) ||
            isPropChanged(accountabilityConfiguration, "ethereum-credentials-file-name", false) ||
            isPropChanged(accountabilityConfiguration, "ethereum-password", false) ||
            isPropChanged(accountabilityConfiguration, "ethereum-provenance-smart-contract-address", true) ||
            isPropChanged(accountabilityConfiguration, "ethereum-authorization-smart-contract-address", true) ||
            isPropChanged(accountabilityConfiguration, "ethereum-permissions-smart-contract-address", true);
    }

    private static boolean isPropChanged(Properties newProps, String name, boolean ignoreCase) {
        if (Strings.hasText(newProps.getProperty(name)))
            if (ignoreCase)
                return newProps.getProperty(name).equalsIgnoreCase(activeProperties.getProperty(name));
            else
                return newProps.getProperty(name).equals(activeProperties.getProperty(name));

        // we are here if new property is empty
        return Strings.isEmpty(activeProperties.getProperty(name));
    }

    private static void copyProperties(Properties accountabilityConfiguration) {
        activeProperties = new Properties();
        accountabilityConfiguration.stringPropertyNames().forEach(name -> activeProperties.setProperty(name, accountabilityConfiguration.getProperty(name)));
    }
}
