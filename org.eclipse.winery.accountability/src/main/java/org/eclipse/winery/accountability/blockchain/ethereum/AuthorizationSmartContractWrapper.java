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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.eclipse.winery.accountability.blockchain.ethereum.generated.Authorization;
import org.eclipse.winery.accountability.exceptions.EthereumException;
import org.eclipse.winery.accountability.model.authorization.AuthorizationElement;
import org.eclipse.winery.accountability.model.authorization.AuthorizationInfo;
import org.eclipse.winery.accountability.model.authorization.AuthorizationTree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.EventValues;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;

/**
 * Provide access to the authorization smart contract
 */
public class AuthorizationSmartContractWrapper extends SmartContractWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationSmartContractWrapper.class);

    AuthorizationSmartContractWrapper(Web3j web3j, Contract contract) {
        super(web3j, contract);
    }

    public CompletableFuture<String> authorize(final String identifier, final String authorizedEthereumAddress,
                                               final String authorizedIndentity) {
        return ((Authorization) contract).authorize(identifier, authorizedEthereumAddress,
            authorizedIndentity)
            .sendAsync()
            // replace the complete receipt with the transaction hash only.
            .thenApply(TransactionReceipt::getTransactionHash);
    }

    /**
     * Retrieves the {@link AuthorizationInfo} from the blockchain.
     * If no authorization data can be retrieved, the completable future returns <code>null</code>.
     *
     * @param identifier The process identifier identifying the collaboration process.
     * @return A completable future containing the authorization information.
     */
    CompletableFuture<AuthorizationInfo> getAuthorizationTree(final String identifier) {
        EthFilter filter = new EthFilter(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST,
            contract.getContractAddress()).
            addSingleTopic(EventEncoder.encode(Authorization.AUTHORIZED_EVENT)).
            addOptionalTopics(Hash.sha3String(identifier)).
            addNullTopic().
            addNullTopic();

        return web3j.ethGetLogs(filter).sendAsync().thenApply(
            ethLog -> {
                final List<EthLog.LogResult> logs = ethLog.getLogs();
                LOGGER.info(logs.size() + " authorization elements detected.");
                final List<AuthorizationElement> authorizationElements = new ArrayList<>();

                try {
                    for (EthLog.LogResult logResult : logs) {
                        final Log log = (Log) logResult.get();
                        final EventValues eventValues =
                            Contract.staticExtractEventParameters(Authorization.AUTHORIZED_EVENT, log);
                        authorizationElements.add(this.generateAuthorizationElement(eventValues, log));
                    }

                    return new AuthorizationTree(authorizationElements);
                } catch (EthereumException e) {
                    throw new CompletionException(e);
                }
            }
        );
    }

    private AuthorizationElement generateAuthorizationElement(EventValues event, Log log) throws EthereumException {
        try {
            final AuthorizationElement result = new AuthorizationElement();
            result.setTransactionHash(log.getTransactionHash());
            // get the timestamp of the block that includes the tx that includes the authorization record
            result.setUnixTimestamp(web3j.ethGetBlockByHash(log.getBlockHash(), false)
                .send()
                .getBlock()
                .getTimestamp()
                .longValue());

            result.setAuthorizerBlockchainAddress((String) event.getIndexedValues().get(1).getValue());
            result.setAuthorizedBlockchainAddress((String) event.getIndexedValues().get(2).getValue());
            result.setAuthorizedIdentity((String) event.getNonIndexedValues().get(0).getValue());

            return result;
        } catch (IOException e) {
            final String msg = "Error while fetching block timestamp. Reason: " + e.getMessage();
            LOGGER.error(msg);
            throw new EthereumException(msg, e);
        }
    }
}
