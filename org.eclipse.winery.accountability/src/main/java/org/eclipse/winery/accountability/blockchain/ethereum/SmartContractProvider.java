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
package org.eclipse.winery.accountability.blockchain.ethereum;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.eclipse.winery.accountability.blockchain.ethereum.generated.Authorization;
import org.eclipse.winery.accountability.blockchain.ethereum.generated.Permissions;
import org.eclipse.winery.accountability.blockchain.ethereum.generated.Provenance;
import org.eclipse.winery.accountability.exceptions.BlockchainException;
import org.eclipse.winery.accountability.exceptions.EthereumException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.tx.Contract;
import org.web3j.tx.gas.DefaultGasProvider;

public class SmartContractProvider {

    private static final Logger log = LoggerFactory.getLogger(EthereumAccessLayer.class);

    private static void validateSmartContract(Contract contract, String address) throws EthereumException {
        try {
            if (!contract.isValid()) {
                final String msg = "Contract at address " + address +
                    " doesn't match the desired contract.";
                log.error(msg);
                throw new EthereumException(msg);
            }
        } catch (IOException e) {
            final String msg = "Error while checking the validity of referenced smart contract. Reason: "
                + e.getMessage();
            log.error(msg);

            throw new EthereumException(msg, e);
        }
    }

    static Provenance buildProvenanceSmartContract(final Web3j web3j, final Credentials credentials, String smartContractAddress) throws BlockchainException {
        Provenance contract = Provenance.load(smartContractAddress, web3j, credentials, new DefaultGasProvider());
        validateSmartContract(contract, smartContractAddress);
        return contract;
    }

    static CompletableFuture<Provenance> deployProvenanceSmartContract(final Web3j web3j, final Credentials credentials) {
        return Provenance.deploy(web3j, credentials, new DefaultGasProvider()).sendAsync();
    }

    static Authorization buildAuthorizationSmartContract(final Web3j web3j, final Credentials credentials, String smartContractAddress) throws BlockchainException {
        Authorization contract = Authorization.load(smartContractAddress, web3j, credentials, new DefaultGasProvider());
        validateSmartContract(contract, smartContractAddress);
        return contract;
    }

    static CompletableFuture<Authorization> deployAuthorizationSmartContract(final Web3j web3j, final Credentials credentials) {
        return Authorization.deploy(web3j, credentials, new DefaultGasProvider()).sendAsync();
    }
    
    static Permissions buildPermissionsSmartContract(final Web3j web3j, final Credentials credentials, String smartContractAddress) throws EthereumException {
        Permissions contract = Permissions.load(smartContractAddress, web3j, credentials, new DefaultGasProvider());
        validateSmartContract(contract, smartContractAddress);
        return contract;
    }

    static CompletableFuture<Permissions> deployPermissionsSmartContract(final Web3j web3j, final Credentials credentials) {
        return Permissions.deploy(web3j, credentials, new DefaultGasProvider()).sendAsync();
    }
}
