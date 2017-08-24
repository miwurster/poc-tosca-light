/*******************************************************************************
 * Copyright (c) 2017 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v10.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *    Karoline Saatkamp - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.eclipse.winery.repository.splitting.groupprovisioninggraphmodel;

import java.util.Set;

import org.eclipse.winery.model.tosca.TNodeTemplate;

public class Group {
	
    private Set<TNodeTemplate> groupComponents;
    private String label;

    public void setGroupComponents(Set<TNodeTemplate> groupComponents) {
        this.groupComponents = groupComponents;
    }
    
    public Set<TNodeTemplate> getGroupComponents () {
        return this.groupComponents;
    }
    
    public void addToGroupComponents (TNodeTemplate nodeTemplate) {
        this.groupComponents.add(nodeTemplate);
    }
    
    public void addAllToGroupComponents(Set<TNodeTemplate> nodeTemplates) {
        this.groupComponents.addAll(nodeTemplates);
    }
    
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
