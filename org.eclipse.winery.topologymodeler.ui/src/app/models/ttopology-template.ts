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
 ********************************************************************************/

export class AbstractTTemplate {
    constructor(public documentation?: any,
                public any?: any,
                public otherAttributes?: any) {
    }
}

/**
 * This is the datamodel for node Templates and relationship templates
 */
export class TTopologyTemplate extends AbstractTTemplate {
    nodeTemplates: Array<TNodeTemplate> = [];
    relationshipTemplates: Array<TRelationshipTemplate> = [];
}

/**
 * This is the datamodel for node Templates
 */
export class TNodeTemplate extends AbstractTTemplate {
    constructor(public properties: any,
                public id: string,
                public type: string,
                public name: string,
                public minInstances: number,
                public maxInstances: number,
                public color: string,
                public imageUrl: string,
                documentation?: any,
                any?: any,
                otherAttributes?: any,
                public x?: string,
                public y?: string,
                public capabilities?: any,
                public requirements?: any,
                public deploymentArtifacts?: any,
                public policies?: any,
                public targetLocations?: any) {
        super(documentation, any, otherAttributes);
    }
}

/**
 * This is the datamodel for the Entity Types
 */
export class EntityType {
    constructor(
        public id: string,
        public qName: string,
        public name: string,
        public namespace: string,
        public color?: string
    ) {}
}

/**
 * This is the datamodel for relationship templates
 */
export class TRelationshipTemplate extends AbstractTTemplate {
    /*
     get targetElement(): string {
     return this.targetElement;
     }
     get sourceElement(): string {
     return this.sourceElement;
     }
     */
    constructor(public sourceElement: { ref: string },
                public targetElement: { ref: string },
                public name?: string,
                public id?: string,
                public type?: string,
                documentation?: any,
                any?: any,
                otherAttributes?: any) {
        super(documentation, any, otherAttributes);
    }

}
/**
 * This is the datamodel for the style of nodes and relationships
 */
export class Visuals {

    constructor(public color: string,
                public nodeTypeId: string,
                public localName?: string,
                public imageUrl?: string) {
    }

    /*
     get color(): string {
     return this._color;
     }

     set color(value: string) {
     this._color = value;
     }

     get nodeTypeId(): string {
     return this._nodeTypeId;
     }

     set nodeTypeId(value: string) {
     this._nodeTypeId = value;
     }

     get localName(): string {
     this._localName = this._nodeTypeId.split('}')[1];
     return this._localName;
     }

     set localName(value: string) {
     this._localName = value;
     }

     get imageUrl(): string {
     return this._imageUrl;
     }

     set imageUrl(value: string) {
     this._imageUrl = value;
     }
     */
}
