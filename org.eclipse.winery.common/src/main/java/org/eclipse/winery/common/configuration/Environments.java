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

package org.eclipse.winery.common.configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import org.eclipse.winery.common.Constants;

import org.apache.commons.configuration2.YAMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class offers Methods for the backend to interact with the Configuration of the winery. There are Methods for
 * either reading or editing certain parts of the configuration file. This Class does not support the addition of
 * properties that are not part of the configuration by default.
 */
public final class Environments {

    private static final Logger LOGGER = LoggerFactory.getLogger(Environments.class);

    private static RepositoryConfigurationObject repositoryConfigurationObject;
    private static GitConfigurationObject gitConfigurationObject;
    private static UiConfigurationObject uiConfigurationObject;

    private Environments() {
    }

    public static void clearInstances() {
        repositoryConfigurationObject = null;
        gitConfigurationObject = null;
        uiConfigurationObject = null;
    }

    /**
     * Returns an instance of the ui configuration.
     *
     * @return Returns an UiConfigurationObject object which represtents the ui configuration of the winery.yml
     * configuration file.
     */
    public static UiConfigurationObject getUiConfig() {
        checkForUpdateAndClear();
        if (uiConfigurationObject == null) {
            uiConfigurationObject = new UiConfigurationObject(Environment.getConfiguration());
        }
        return uiConfigurationObject;
    }

    /**
     * Returns an instance of the git configuration.
     *
     * @return Returns a GitConfigurationObject object which represtents the git configuration of the winery.yml
     * configuration file.
     */
    public static GitConfigurationObject getGitConfig() {
        checkForUpdateAndClear();
        if (gitConfigurationObject == null) {
            gitConfigurationObject = new GitConfigurationObject(Environment.getConfiguration());
        }
        return gitConfigurationObject;
    }

    /**
     * Returns an instance of the repository configuration. This includes the GitConfigurationObject
     *
     * @return Returns a RepositoryConfigurationObject object which represtents the repository configuration of the
     * winery.yml configuration file.
     */
    public static RepositoryConfigurationObject getRepositoryConfig() {
        checkForUpdateAndClear();
        if (repositoryConfigurationObject == null) {
            repositoryConfigurationObject = new RepositoryConfigurationObject(Environment.getConfiguration());
        }
        return repositoryConfigurationObject;
    }

    /**
     * Method to retrieve the set version.
     *
     * @return the version declared in the pom file in case of an exception returns the version 0.0.0
     */
    public static String getVersion() {
        try {
            return new Environments().getVersionFromProperties();
        } catch (IOException e) {
            LOGGER.debug("Error while retrieving version from pom.", e);
        }
        return "0.0.0";
    }

    /**
     * Checks the configuration file for an update and clears the configuration object instances, so that they will be
     * reloaded from the changed configuration file when their corresponding getter is invoked.
     */
    private static void checkForUpdateAndClear() {
        if (Environment.checkConfigurationForUpdate()) {
            Environments.clearInstances();
        }
    }

    /**
     * Returns a FileBasedRepositoryConfiguration
     *
     * @return an instance of FileBasedRepositoryConfiguration
     */
    public static FileBasedRepositoryConfiguration getFilebasedRepositoryConfiguration() {
        Path path = Paths.get(getRepositoryConfig().getRepositoryRoot());
        return new FileBasedRepositoryConfiguration(path);
    }

    /**
     * Returns a GitBasedRepositoryConfiguration
     *
     * @return an instance of GitBasedRepositoryConfiguration
     */
    public static Optional<GitBasedRepositoryConfiguration> getGitBasedRepositoryConfiguration() {
        final FileBasedRepositoryConfiguration filebasedRepositoryConfiguration = getFilebasedRepositoryConfiguration();
        return Optional.of(new GitBasedRepositoryConfiguration(getGitConfig().isAutocommit(), filebasedRepositoryConfiguration));
    }

//    /**
//     * This method propagates changes made to the feature flags to the config file.
//     *
//     * @param changedProperties a Map that contains the name of the changed properties as keys and the changed flags as
//     *                          values as
//     */
//    public static void saveFeatures(final ConfigurationObject changedProperties) {
//        YAMLConfiguration config = Environment.getConfiguration();
//        changedProperties.getFeatures().keySet().stream()
//            .filter(p -> !RepositoryProvider.YAML.equals(Enums.valueOf(RepositoryProvider.class, p)))
//            .forEach(property -> config.setProperty(featurePrefix + property, changedProperties.getFeatures().get(property)));
//        Environment.save();
//    }

    /**
     * Changes the configuration accordingly to the given ConfigurationObject.
     *
     * @param configuration the changed configuration object
     */
    public static void save(final AbstractConfigurationObject configuration) {
        configuration.save();
    }

    private String getVersionFromProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(this.getClass().getClassLoader().getResourceAsStream("version.properties"));
        return properties.getProperty("version");
    }
}
