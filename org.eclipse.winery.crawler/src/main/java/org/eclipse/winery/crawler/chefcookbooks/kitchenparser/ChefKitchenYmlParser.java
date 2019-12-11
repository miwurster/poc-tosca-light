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

package org.eclipse.winery.crawler.chefcookbooks.kitchenparser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.winery.crawler.chefcookbooks.chefcookbook.CookbookParseResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

public class ChefKitchenYmlParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChefKitchenYmlParser.class);

    private Map<String, Object> kitchenYml;
    private String cookbookName;
    private String cookbookPath;

    public ChefKitchenYmlParser(CookbookParseResult cookbookParseResult) {
        this.cookbookName = cookbookParseResult.getCookbookName();
        this.cookbookPath = cookbookParseResult.getCookbookPath();

        this.kitchenYml = parseKitchen();
    }

    private Map<String, Object> parseKitchen() {

        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);

        try (InputStream inputStream = new FileInputStream(this.cookbookPath + "/kitchen.yml")) {
            return new Yaml().load(inputStream);
        } catch (FileNotFoundException e1) {
            try (InputStream inputStream = new FileInputStream(this.cookbookPath + "/.kitchen.yml")) {
                return new Yaml().load(inputStream);
            } catch (IOException e2) {
                LOGGER.error("Cookbook \"" + cookbookName + "\"" + " has no .kitchen.yml file");
            }
        } catch (IOException e) {
            LOGGER.error("Cookbook \"" + cookbookName + "\"" + " has no kitchen.yml file", e);
        }

        return null;
    }

    /**
     * @return List with all platform configurations from kitchen.yml each List item is a Map with a platform
     * configuration
     */
    public List<Map<String, String>> getPlatforms() {

        if (this.kitchenYml != null) {
            return (List<Map<String, String>>) kitchenYml.get("platforms");
        } else return null;
    }

    /**
     * @return List with all platform names from kitchen.yml Platform name includes the platform version
     */
    public List<String> getPlatformNames() {
        List<Map<String, String>> platformConfig = this.getPlatforms();
        if (platformConfig != null) {
            List<String> platformNames = new ArrayList<>();
            for (Map<String, String> stringStringMap : platformConfig) {
                platformNames.add(stringStringMap.get("name"));
            }
            return platformNames;
        } else return null;
    }
}
