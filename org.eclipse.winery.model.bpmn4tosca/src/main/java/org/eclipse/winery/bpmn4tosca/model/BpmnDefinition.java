/*******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.bpmn4tosca.model;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.TBoolean;
import org.eclipse.winery.model.tosca.TEntityType;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.kvproperties.PropertyDefinitionKV;
import org.eclipse.winery.model.tosca.kvproperties.PropertyDefinitionKVList;
import org.eclipse.winery.model.tosca.kvproperties.WinerysPropertiesDefinition;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;

import static org.eclipse.winery.model.tosca.constants.Namespaces.URI_BPMN20_MODEL;
import static org.eclipse.winery.model.tosca.constants.Namespaces.URI_BPMN4TOSCA_20;

public class BpmnDefinition {

    public static QName Activity = new QName(URI_BPMN20_MODEL, "Activity");
    public static QName Gateway = new QName(URI_BPMN20_MODEL, "Gateway");
    public static QName Event = new QName(URI_BPMN20_MODEL, "Event");

    public static QName SequenceFlow = new QName(URI_BPMN20_MODEL, "SequenceFlow");
    public static QName ExecuteAfter = new QName(URI_BPMN4TOSCA_20, "ExecuteAfter");
    public static QName ExecuteBefore = new QName(URI_BPMN4TOSCA_20, "ExecuteBefore");

    public static Map<QName, TEntityType> createBpmnTypes() {
        HashMap<QName, TEntityType> bpmnTypes = new HashMap<>();

        addAbstractNodeTypes(bpmnTypes);
        addRelationTypes(bpmnTypes);

        QName taskQName = new QName(URI_BPMN20_MODEL, "Task");
        TNodeType task = new TNodeType(taskQName);
        task.setDerivedFrom(new TNodeType.DerivedFrom(Activity));
//        TInterface taskInterface = new TInterface("BPMNInterface");
//        taskInterface.getOperation()
//            .add(new TOperation("imperativeTask"));
//        TInterfaces tInterfaces = new TInterfaces();
//        tInterfaces.getInterface().add(taskInterface);
//        task.setInterfaces(tInterfaces);
        PropertyDefinitionKVList definitionKVList = new PropertyDefinitionKVList();
        definitionKVList.add(new PropertyDefinitionKV("nodeTemplateId", "xsd:string"));
        definitionKVList.add(new PropertyDefinitionKV("interfaceName", "xsd:string"));
        definitionKVList.add(new PropertyDefinitionKV("operationName", "xsd:string"));
        WinerysPropertiesDefinition winerysPropertiesDefinition = new WinerysPropertiesDefinition();
        winerysPropertiesDefinition.setPropertyDefinitionKVList(definitionKVList);
        ModelUtilities.replaceWinerysPropertiesDefinition(task, winerysPropertiesDefinition);
        bpmnTypes.put(taskQName, task);

        QName parallelQName = new QName(URI_BPMN20_MODEL, "Parallel");
        TNodeType parallel = new TNodeType(parallelQName);
        parallel.setDerivedFrom(new TNodeType.DerivedFrom(Gateway));
        bpmnTypes.put(parallelQName, parallel);

        QName terminationQName = new QName(URI_BPMN20_MODEL, "Termination");
        TNodeType termination = new TNodeType(terminationQName);
        termination.setDerivedFrom(new TNodeType.DerivedFrom(Event));
        bpmnTypes.put(terminationQName, termination);

        return bpmnTypes;
    }

    private static void addRelationTypes(HashMap<QName, TEntityType> bpmnTypes) {
        TRelationshipType bpmnRelationType = new TRelationshipType(SequenceFlow);
        bpmnTypes.put(SequenceFlow, bpmnRelationType);

        TRelationshipType executeAfterRelationType = new TRelationshipType(ExecuteAfter);
        bpmnTypes.put(ExecuteAfter, executeAfterRelationType);

        TRelationshipType executeBeforeRelationType = new TRelationshipType(ExecuteBefore);
        bpmnTypes.put(ExecuteBefore, executeBeforeRelationType);
    }

    private static void addAbstractNodeTypes(HashMap<QName, TEntityType> bpmnTypes) {
        TNodeType activityNodeType = new TNodeType(Activity);
        activityNodeType.setAbstract(TBoolean.YES);
        bpmnTypes.put(Activity, activityNodeType);

        TNodeType gatewayNodeType = new TNodeType(Gateway);
        gatewayNodeType.setAbstract(TBoolean.YES);
        bpmnTypes.put(Gateway, gatewayNodeType);

        TNodeType eventNodeType = new TNodeType(Event);
        gatewayNodeType.setAbstract(TBoolean.YES);
        bpmnTypes.put(Event, eventNodeType);
    }
}
