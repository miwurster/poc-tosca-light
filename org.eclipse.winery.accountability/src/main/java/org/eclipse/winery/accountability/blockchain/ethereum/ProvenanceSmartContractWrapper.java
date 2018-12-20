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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.eclipse.winery.accountability.blockchain.ethereum.generated.Provenance;
import org.eclipse.winery.accountability.blockchain.util.CompressionUtils;
import org.eclipse.winery.accountability.exceptions.EthereumException;
import org.eclipse.winery.accountability.model.ModelProvenanceElement;

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
 * Provide access to the functionality of the provenance smart contract
 */
class ProvenanceSmartContractWrapper extends SmartContractWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvenanceSmartContractWrapper.class);

    ProvenanceSmartContractWrapper(Web3j web3j, Contract contract) {
        super(web3j, contract);
    }

    CompletableFuture<String> saveState(final String identifier, final String state) {
        LocalDateTime start = LocalDateTime.now();
        byte[] compressed = CompressionUtils.compress(state.getBytes());
        LOGGER.debug("Compressing fingerprint lasted {}", Duration.between(LocalDateTime.now(), start).toString());

        return ((Provenance) contract).addResourceVersion(identifier, compressed)
            .sendAsync()
            // replace the complete receipt with the transaction hash only.
            .thenApply(TransactionReceipt::getTransactionHash);
    }

    CompletableFuture<List<ModelProvenanceElement>> getProvenance(final String identifier) {
        EthFilter filter = new EthFilter(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST,
            contract.getContractAddress()).
            addSingleTopic(EventEncoder.encode(Provenance.RESOURCEVERSION_EVENT)).
            addOptionalTopics(Hash.sha3String(identifier)).
            addNullTopic();

        return web3j.ethGetLogs(filter).sendAsync().thenApply(
            ethLog -> {
                final List<EthLog.LogResult> logs = ethLog.getLogs();
                LOGGER.info(logs.size() + " provenance elements detected.");
                final List<ModelProvenanceElement> result = new ArrayList<>();

                try {
                    for (EthLog.LogResult logResult : logs) {
                        final Log log = (Log) logResult.get();
                        final EventValues eventValues =
                            Contract.staticExtractEventParameters(Provenance.RESOURCEVERSION_EVENT, log);
                        result.add(this.generateProvenanceElement(eventValues, log));
                    }
                } catch (EthereumException e) {
                    throw new CompletionException(e);
                }

                return result;
            }
        );
    }

    private ModelProvenanceElement generateProvenanceElement(EventValues event, Log log) throws EthereumException {
        try {
            final ModelProvenanceElement result = new ModelProvenanceElement();
            result.setTransactionHash(log.getTransactionHash());
            result.setAuthorAddress((String) event.getIndexedValues().get(1).getValue());
            // decompress the state
            final byte[] compressedState = (byte[]) event.getNonIndexedValues().get(0).getValue();
            result.setFingerprint(new String(CompressionUtils.decompress(compressedState), StandardCharsets.UTF_8));
            // get the timestamp of the block that includes the tx that includes the state change
            result.setUnixTimestamp(web3j.ethGetBlockByHash(log.getBlockHash(), false)
                .send()
                .getBlock()
                .getTimestamp()
                .longValue());

            return result;
        } catch (IOException e) {
            final String msg = "Error while fetching block timestamp. Reason: " + e.getMessage();
            LOGGER.error(msg);
            throw new EthereumException(msg, e);
        }
    }
}
