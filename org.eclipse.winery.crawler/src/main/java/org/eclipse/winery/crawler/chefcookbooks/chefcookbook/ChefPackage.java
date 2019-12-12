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

package org.eclipse.winery.crawler.chefcookbooks.chefcookbook;

import org.eclipse.winery.crawler.chefcookbooks.constants.ChefDslConstants;

public class ChefPackage extends VersionedChefElement {

    private String packageName;
    // Action of the package. 
    // See: https://docs.chef.io/resource_package.html
    private String action;

    public ChefPackage(String name) {
        this(name, null);
        this.packageName = name;
        this.action = ":install";
    }

    public ChefPackage(String name, String version) {
        super(name, version);
        this.packageName = name;
        this.action = ":install";
    }

    public ChefPackage(ChefPackage chefPackage) {
        this(chefPackage.name, chefPackage.version);
        this.packageName = chefPackage.packageName;
        this.action = chefPackage.action;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * This method adds properties to a chef package in the correct way.
     */
    public void addProperty(String propertyName, String propertyValue) {
        switch (propertyName) {
            case ChefDslConstants.PACKAGE_NAME_PROPERTY:
                setPackageName(propertyValue);
                break;
            case ChefDslConstants.PACKAGE_VERSION_PROPERTY:
                setVersion(propertyValue);
                break;
            case "action":
                setAction(propertyValue);
                break;
        }
    }
}
