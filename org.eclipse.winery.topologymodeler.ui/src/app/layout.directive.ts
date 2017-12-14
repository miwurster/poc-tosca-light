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

import { AfterViewInit, Directive, ElementRef } from '@angular/core';
import ELK from 'elkjs/lib/elk.bundled.js';
import { TNodeTemplate, TRelationshipTemplate } from './models/ttopology-template';
import { WineryAlertService } from './winery-alert/winery-alert.service';
import { JsPlumbService } from './jsPlumbService';
import {LayoutChildNodeModel} from './models/layoutChildNodeModel';

@Directive({
  selector: '[wineryLayout]'
})
/**
 * Manages all layouting operations besides drag and drop (this is in canvas.ts)
 */
export class LayoutDirective implements AfterViewInit {
    private jsPlumbInstance: any;
    readonly nodeXOffset = 40;
    readonly nodeYOffset = 50;

  constructor(private alert: WineryAlertService,
              private elRef: ElementRef,
              private jsPlumbService: JsPlumbService) {
      this.jsPlumbInstance = this.jsPlumbService.getJsPlumbInstance();
      this.jsPlumbInstance.setContainer('container');

  }

  /**
   * Layouts all nodes (not just the selected ones).
   * Uses ELK.Js which implements sugiyama to layout nodes.
   * @param nodeTemplates
   * @param relationshipTemplates
   * @param jsPlumbInstance
   */
  public layoutNodes(nodeTemplates: Array<TNodeTemplate>,
                     relationshipTemplates: Array<TRelationshipTemplate>,
                     jsPlumbInstance: any): void {
    // These are the input arrays for eclipse layout kernel (ELK).
    const children: LayoutChildNodeModel[] = [];
    const edges: any[] = [];

    // get width and height of nodes
    nodeTemplates.forEach((node) => {
      const width = this.elRef.nativeElement.querySelector('#' + node.id).offsetWidth;
      const height = this.elRef.nativeElement.querySelector('#' + node.id).offsetHeight;
      children.push(new LayoutChildNodeModel(node.id, width, height));
      // also get their current positions and apply them to the internal list
      const left = this.elRef.nativeElement.querySelector('#' + node.id).offsetLeft;
      const top = this.elRef.nativeElement.querySelector('#' + node.id).offsetTop;
      node.x = left;
      node.y = top;
    });

    // get source and targets of relationships
    relationshipTemplates.forEach((rel, index) => {
      const sourceElement = rel.sourceElement;
      const targetElement = rel.targetElement;
      edges.push({id: index.toString(), sources: [sourceElement], targets: [targetElement]});
    });

    // initialize elk object which will layout the graph
    const elk = new ELK({});
    const graph = {
      id: 'root',
      properties: {
        'elk.algorithm': 'layered',
        'elk.spacing.nodeNode': '200',
        'elk.direction': 'DOWN',
        'elk.layered.spacing.nodeNodeBetweenLayers': '200'
      },
      children: children,
      edges: edges,
    };

    const promise = elk.layout(graph);
    promise.then((data) => {
      this.applyPositions(data, nodeTemplates, this.jsPlumbInstance);
    });
  }

  /**
   * This applies the calculated positions to the actual node elements.
   * Uses ELK.Js which implements sugiyama to layout nodes.
   * @param data The data (relationships, nodes) used by the layouting algo.
   * @param nodeTemplates The internal representation of the nodes.
   * @param jsPlumbInstance
   */
  private applyPositions(data: any,
                         nodeTemplates: Array<TNodeTemplate>,
                         jsPlumbInstance: any): void {
    nodeTemplates.forEach((node, index) => {
      // apply the new positions to the nodes
      node.x = data.children[index].x + this.nodeXOffset;
      node.y = data.children[index].y + 50;
    });

    this.repaintEverything(this.jsPlumbInstance);
  }

  /**
   * Aligns all selected elements horizontally.
   * If no element is selected, all elements get aligned horizontal.
   * @param selectedNodes
   * @param jsPlumbInstance
   */
  public alignHorizontal(selectedNodes: Array<TNodeTemplate>,
                         jsPlumbInstance: any): void {
    let result;
    // if there is only 1 node selected, do nothing
    if (!( selectedNodes.length === 1)) {
      const topPositions = selectedNodes.map((node) => {
        return this.elRef.nativeElement.querySelector('#' + node.id).offsetTop;
      });
      // add biggest value to smallest and divide by 2, to get the exact middle of both
      result = ((Math.max.apply(null, topPositions) + Math.min.apply(null, topPositions)) / 2);
      // iterate over the nodes again, and apply positions
      selectedNodes.forEach((node) => {
        node.y = result;
      });
      this.repaintEverything(this.jsPlumbInstance);
    } else {
      this.showWarningAlert('You have only one node selected.');
    }
  }

  /**
   * Aligns all selected elements vertically.
   * If no element is selected, all elements get aligned vertical.
   * @param selectedNodes
   * @param jsPlumbInstance
   */
  public alignVertical(selectedNodes: Array<TNodeTemplate>,
                       jsPlumbInstance: any): void {
    let result;
    // if there is only 1 node selected, do nothing
    if (!( selectedNodes.length === 1)) {
      const topPositions = selectedNodes.map((node) => {
        return this.elRef.nativeElement.querySelector('#' + node.id).offsetLeft;
      });
      // add biggest value to smallest and divide by 2, to get the exact middle of both
      result = ((Math.max.apply(null, topPositions) + Math.min.apply(null, topPositions)) / 2);
      // iterate over the nodes again, and apply positions
      selectedNodes.forEach((node) => {
        node.x = result;
      });
      this.repaintEverything(this.jsPlumbInstance);
    } else {
      this.showWarningAlert('You have only one node selected.');
    }
  }

  /**
   * Repaints everything after 1ms.
   * @param jsPlumbInstance
   */
  private repaintEverything(jsPlumbInstance: any): void {
    setTimeout(() => this.jsPlumbInstance.repaintEverything(), 1);
  }

  /**
   * Shows a warning.
   * @param message The message which is displayed.
   */
  private showWarningAlert(message: string): void {
    this.alert.info(message);
  }

  /**
   * Angular lifecycle event
   */
  ngAfterViewInit() {

  }
}
