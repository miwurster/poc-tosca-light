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

import java.util.LinkedList;

import org.jgrapht.graph.DefaultDirectedGraph;

public class GroupProvisioningOrderGraph extends DefaultDirectedGraph<Group, OrderRelation> {

	public GroupProvisioningOrderGraph() {
		super(OrderRelation.class);
	}

	public void removeAllEdges(GroupProvisioningOrderGraph graph) {
		LinkedList<OrderRelation> copy = new LinkedList<OrderRelation>();
		for (OrderRelation e : graph.edgeSet()) {
			copy.add(e);
		}
		graph.removeAllEdges(copy);
	}

	public void clearGraph() {
		removeAllEdges(this);
		removeAllVertices(this);
	}


	public void removeAllVertices(GroupProvisioningOrderGraph graph) {
		LinkedList<Group> copy = new LinkedList<Group>();
		for (Group v : graph.vertexSet()) {
			copy.add(v);
		}
		graph.removeAllVertices(copy);
	}
}
