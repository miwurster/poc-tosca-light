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

import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { animate, state, style, transition, trigger } from '@angular/animations';
import { PaletteService } from '../palette.service';
import { WineryActions } from '../redux/actions/winery.actions';
import { NgRedux } from '@angular-redux/store';
import { IWineryState } from '../redux/store/winery.store';
import { TNodeTemplate } from '../models/ttopology-template';
import {BackendService} from '../backend.service';

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
    detailsAreHidden = true;
    paletteRootState = 'extended';
    paletteItems = [];
    allNodeTemplates: Array<TNodeTemplate> = [];
    nodeTemplatesSubscription;
    paletteOpenedSubscription;
    public oneAtATime = true;
    // All Node Types grouped by their namespaces
    groupedNodeTypes = [];

    constructor(private paletteService: PaletteService,
                private ngRedux: NgRedux<IWineryState>,
                private actions: WineryActions,
                private backendService: BackendService) {
        this.nodeTemplatesSubscription = ngRedux.select(wineryState => wineryState.wineryState.currentJsonTopology.nodeTemplates)
            .subscribe(currentNodes => this.updateNodes(currentNodes));
        this.paletteOpenedSubscription = this.ngRedux.select(wineryState => wineryState.wineryState.currentPaletteOpenedState)
            .subscribe(currentPaletteOpened => this.updateState(currentPaletteOpened));
        this.paletteItems = paletteService.getPaletteData();
        this.backendService.groupedNodeTypes$.subscribe(data => {
            this.groupedNodeTypes = data;
        });
    }

    /**
     * Gets called if nodes get deleted or created and calls the
     * correct handler.
     * @param currentNodes  List of all displayed nodes.
     */
    updateNodes(currentNodes: Array<TNodeTemplate>): void {
        if (currentNodes.length > 0) {
            this.allNodeTemplates = currentNodes;
        }
    }

    /**
     * Applies the correct css, depending on if the palette is open or not.
     * @param newPaletteOpenedState
     */
    updateState(newPaletteOpenedState: any) {
        if (!newPaletteOpenedState) {
            this.paletteRootState = 'shrunk';
        } else {
            this.paletteRootState = 'extended';
        }
    }

    /**
     * Angular lifecycle event.
     */
    ngOnInit() {
    }

    /**
     * opens the palette if its closed.
     */
    public openPalette(): void {
        this.detailsAreHidden = false;
        this.toggleRootState();
    }

    /**
     * opens the palette if its closed and vice versa.
     */
    private toggleRootState(): void {
        if (this.paletteRootState === 'shrunk') {
            this.ngRedux.dispatch(this.actions.sendPaletteOpened(true));
        } else {
            this.ngRedux.dispatch(this.actions.sendPaletteOpened(false));
        }
    }

    /**
     * Generates and publishes a new node.
     * @param $event
     */
    publishTitle($event): void {
        const left = ($event.pageX - 108).toString();
        const top = ($event.pageY - 30).toString();
        const name = $event.target.innerHTML;
        const otherAttributes = {
            location: 'undefined',
            x: left,
            y: top
        };
        const y = top;
        const x = left;
        const newIdType = this.generateId(name);
        const newId = newIdType.newId;
        const newType = newIdType.type;
        console.log(newId);
        const paletteItem: TNodeTemplate = new TNodeTemplate(
            undefined,
            newId,
            newType,
            name,
            1,
            1,
            'yellow',
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
        this.ngRedux.dispatch(this.actions.saveNodeTemplate(paletteItem));
        this.ngRedux.dispatch(this.actions.sendPaletteOpened(false));
    }

    /**
     * Generates a new node id, which must be unique.
     * @param name
     */
    generateId(name: string): any {
        if (this.allNodeTemplates.length > 0) {
            for (let i = this.allNodeTemplates.length - 1; i >= 0; i--) {
                const type = this.allNodeTemplates[i].type;
                const typeOfCurrentNode = type.split('}').pop();
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
                        newId: newId,
                        type: type
                    };
                    return result;
                }
            }
            return name;
        } else {
            return name;
        }
    }

    /**
     * Angular lifecycle event.
     */
    ngOnDestroy() {
        this.nodeTemplatesSubscription.unsubscribe();
        this.paletteOpenedSubscription.unsubscribe();
    }
}


