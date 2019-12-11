/*******************************************************************************
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

package org.eclipse.winery.crawler.chefcookbooks;

import org.eclipse.winery.crawler.chefcookbooks.chefcookbookcrawler.ChefSupermarketCrawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        ChefSupermarketCrawler chefSupermarketCrawler = new ChefSupermarketCrawler();

        long start = System.currentTimeMillis();
        LOGGER.info("Starting to download cookbooks");
        try {
            chefSupermarketCrawler.getAvailableCookbooksFast();
            LOGGER.info("Done downloading...");
        } catch (Exception e) {
            LOGGER.error("Error while downloading...", e);
            logTime(start);
            System.exit(0);
        }

        logTime(start);

        start = System.currentTimeMillis();
        LOGGER.info("Starting to analyze data...");
        ChefCookbookAnalyzer chefCookbookAnalyzer = new ChefCookbookAnalyzer();
        try {
            chefCookbookAnalyzer.main();
            LOGGER.info("Done analyzing...");
        } catch (Exception e) {
            LOGGER.error("Error while analyzing...", e);
        }
        logTime(start);
    }

    private static void logTime(long start) {
        long end = System.currentTimeMillis();
        LOGGER.info("Time needed: {} hours {} minutes and {} seconds", (int) ((end - start) / 1000 / 60 / 60), (int) ((end - start) / 1000 / 60) % 60, (int) ((end - start) / 1000) % 60);
    }
}
