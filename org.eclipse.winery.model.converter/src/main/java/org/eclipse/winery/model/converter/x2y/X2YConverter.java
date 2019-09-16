/********************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.model.converter.x2y;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.eclipse.winery.common.ids.definitions.ArtifactTypeId;
import org.eclipse.winery.common.ids.definitions.CapabilityTypeId;
import org.eclipse.winery.common.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.common.ids.definitions.NodeTypeId;
import org.eclipse.winery.common.ids.definitions.PolicyTypeId;
import org.eclipse.winery.common.ids.definitions.RelationshipTypeId;
import org.eclipse.winery.common.ids.definitions.RequirementTypeId;
import org.eclipse.winery.model.tosca.Definitions;
import org.eclipse.winery.model.tosca.TEntityType;
import org.eclipse.winery.model.tosca.TRequirementType;
import org.eclipse.winery.model.tosca.TTag;
import org.eclipse.winery.model.tosca.TTags;
import org.eclipse.winery.model.tosca.yaml.TNodeType;
import org.eclipse.winery.model.tosca.yaml.support.Metadata;

import javafx.beans.binding.ObjectExpression;

public class X2YConverter {

    public X2YConverter() {}
    
    public InputStream covertX2Y(InputStream inputStream) {
        Definitions definitions = parseXML(inputStream);
        
        return null;
    }
    
    private Definitions parseXML(InputStream inputStream) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Definitions.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (Definitions) unmarshaller.unmarshal(inputStream);
            
        } catch (JAXBException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private <T, K>Map<String, K> convert(T node) {
        if (node instanceof org.eclipse.winery.model.tosca.TNodeType) {
            
        }
        
    }
    
    private Map<String, TNodeType> convert(org.eclipse.winery.model.tosca.TNodeType node) {
        if (Objects.isNull(node)) return null;
        
    }


    public Metadata convert(TTags node) {
        if (Objects.isNull(node)) return null;
        return node.getTag().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(
                TTag::getName,
                TTag::getValue,
                (a, b) -> a + "|" + b,
                Metadata::new));
    }

/*    public QName convert(TEntityType.DerivedFrom node, Class<? extends TEntityType> clazz) {
        if (Objects.isNull(node)) return null;
        DefinitionsChildId id;
        if (clazz.equals(org.eclipse.winery.model.tosca.TNodeType.class)) {
            id = new NodeTypeId(node.getTypeRef());
        } else if (clazz.equals(org.eclipse.winery.model.tosca.TRelationshipType.class)) {
            id = new RelationshipTypeId(node.getTypeRef());
        } else if (clazz.equals(TRequirementType.class)) {
            id = new RequirementTypeId(node.getTypeRef());
        } else if (clazz.equals(org.eclipse.winery.model.tosca.TCapabilityType.class)) {
            id = new CapabilityTypeId(node.getTypeRef());
        } else if (clazz.equals(org.eclipse.winery.model.tosca.TArtifactType.class)) {
            id = new ArtifactTypeId(node.getTypeRef());
        } else {
            id = new PolicyTypeId(node.getTypeRef());
        }
        return getQName(
            id,
            node.getTypeRef().getNamespaceURI(),
            node.getTypeRef().getLocalPart());
    }*/

/*    private QName getQName(DefinitionsChildId id, String namespaceURI, String localPart) {
        setImportDefinition(id);
        return new QName(
            namespaceURI,
            localPart,
            this.getNamespacePrefix(namespaceURI)
        );
    }*/
    
    
    
    
}
