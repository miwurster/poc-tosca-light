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

import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {animate, state, style, transition, trigger} from '@angular/animations';
import {PaletteService} from '../palette.service';
import {WineryActions} from '../redux/actions/winery.actions';
import {NgRedux} from '@angular-redux/store';
import {IWineryState} from '../redux/store/winery.store';
import {TNodeTemplate} from '../models/ttopology-template';
import {BackendService} from '../backend.service';
import {NewNodeIdTypeColorModel} from '../models/newNodeIdTypeColorModel';

/**
 * This is the left sidebar, where nodes can be created from.
 */
@Component({
    selector: 'winery-palette-component',
    templateUrl: './palette.component.html',
    styleUrls: ['./palette.component.css'],
    providers: [PaletteService],
    animations: [
        trigger('paletteItemState', [
            state('shrunk', style({
                display: 'none',
                opacity: '0',
                height: '0px',
            })),
            state('extended', style({
                display: 'block',
                opacity: '1',
                height: '100%',
            })),
            transition('shrunk => extended', animate('100ms ease-out')),
            transition('extended => shrunk', animate('100ms ease-out'))
        ])
    ]
})
export class PaletteComponent implements OnInit, OnDestroy {
    @Input() entityTypes;
    paletteRootState = 'extended';
    nodeTemplatesSubscription;
    paletteOpenedSubscription;
    public oneAtATime = true;
    allNodeTemplates: TNodeTemplate[] = [];
    readonly newNodePositionOffsetX = 108;
    readonly newNodePositionOffsetY = 30;

    constructor (private ngRedux: NgRedux<IWineryState>,
                 private actions: WineryActions,
                 private backendService: BackendService) {
        this.nodeTemplatesSubscription = ngRedux.select(wineryState => wineryState.wineryState.currentJsonTopology.nodeTemplates)
            .subscribe(currentNodes => this.updateNodes(currentNodes));
        this.paletteOpenedSubscription = ngRedux.select(wineryState => wineryState.wineryState.currentPaletteOpenedState)
            .subscribe(currentPaletteOpened => this.updateState(currentPaletteOpened));
    }

    /**
     * Applies the correct css, depending on if the palette is open or not.
     * @param newPaletteOpenedState
     */
    updateState (newPaletteOpenedState: any) {
        if (!newPaletteOpenedState) {
            this.paletteRootState = 'shrunk';
        } else {
            this.paletteRootState = 'extended';
        }
    }

    /**
     * Angular lifecycle event.
     */
    ngOnInit () {
    }

    /**
     * opens the palette if its closed and vice versa.
     */
    public toggleRootState (): void {
        if (this.paletteRootState === 'shrunk') {
            this.ngRedux.dispatch(this.actions.sendPaletteOpened(true));
        } else {
            this.ngRedux.dispatch(this.actions.sendPaletteOpened(false));
        }
    }

    /**
     * Gets called if nodes get deleted or created and calls the
     * correct handler.
     * @param currentNodes  List of all displayed nodes.
     */
    updateNodes (currentNodes: Array<TNodeTemplate>): void {
        this.allNodeTemplates = currentNodes;
    }

    /**
     * Generates and stores a new node in the store.
     * @param $event
     */
    generateNewNode ($event): void {
        const left = ($event.pageX - this.newNodePositionOffsetX).toString();
        const top = ($event.pageY - this.newNodePositionOffsetY).toString();
        const name = $event.target.innerText;
        const otherAttributes = {
            location: 'undefined',
            x: left,
            y: top
        };
        const y = top;
        const x = left;
        const newIdTypeColor = this.generateIdTypeColor(name);
        const newId = newIdTypeColor.id;
        const newType = newIdTypeColor.type;
        const newNode: TNodeTemplate = new TNodeTemplate(
            undefined,
            newId,
            newType,
            name,
            1,
            1,
            newIdTypeColor.color,
            undefined,
            undefined,
            undefined,
            otherAttributes,
            x,
            y,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined
        );
        this.ngRedux.dispatch(this.actions.saveNodeTemplate(newNode));
    }

    /**
     * Generates a new unique node id, type and color.
     * @param name
     * @return result
     */
    generateIdTypeColor (name: string): NewNodeIdTypeColorModel {
        if (this.allNodeTemplates.length > 0) {
            // iterate from back to front because only the last added instance of a node type is important
            // e.g. Node_8 so to increase to Node_9 only the 8 is important which is in the end of the array
            for (let i = this.allNodeTemplates.length - 1; i >= 0; i--) {
                // get type of node Template
                const type = this.allNodeTemplates[i].type;
                const color = this.allNodeTemplates[i].color;
                // split it to get a string like "NodeTypeWithTwoProperties"
                let typeOfCurrentNode = type.split('}').pop();
                // eliminate whitespaces from both strings, important for string comparison
                typeOfCurrentNode = typeOfCurrentNode.replace(/\s+/g, '');
                name = name.replace(/\s+/g, '');
                if (name === typeOfCurrentNode) {
                    const idOfCurrentNode = this.allNodeTemplates[i].id;
                    const numberOfNewInstance = parseInt(idOfCurrentNode.substring(idOfCurrentNode.indexOf('_') + 1), 10) + 1;
                    let newId;
                    if (numberOfNewInstance) {
                        newId = name.concat('_', numberOfNewInstance.toString());
                    } else {
                        newId = name.concat('_', '2');
                    }
                    const result = {
                        id: newId,
                        type: type,
                        color: color
                    };
                    return result;
                }
            }
            return this.getNewNodeDataFromNodeTypes(name);
        } else {
            return this.getNewNodeDataFromNodeTypes(name);
        }
    }

    private getNewNodeDataFromNodeTypes (name: string): any {
        // case that the node name is not in the array which contains a local copy of all node templates visible in the DOM,
        // then search in ungroupedNodeTypes where all possible node information is available
        for (const node of this.entityTypes.unGroupedNodeTypes) {
            if (node.id === name) {
                const result = {
                    id: node.id,
                    type: node.qName,
                    color: node.color
                };
                return result;
            }
        }
    }

    /**
     * Angular lifecycle event.
     */
    ngOnDestroy () {
        this.nodeTemplatesSubscription.unsubscribe();
        this.paletteOpenedSubscription.unsubscribe();
    }
}


