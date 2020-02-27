/*******************************************************************************
 * Copyright (c) 2018-2020 Contributors to the Eclipse Foundation
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
import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { backendBaseURL } from '../../configuration';
import { SectionData } from '../sectionData';
import { ExistService } from '../../wineryUtils/existService';
import { ModalDirective } from 'ngx-bootstrap';
import { Router } from '@angular/router';
import { EntityService } from './entity.service';
import { ToscaTypes } from '../../model/enums';
import { WineryNotificationService } from '../../wineryNotificationModule/wineryNotification.service';
import { TargetAllocationComponent } from '../../wineryTargetAllocation/targetAllocation.component';
import { WineryRepositoryConfigurationService } from '../../wineryFeatureToggleModule/WineryRepositoryConfiguration.service';

@Component({
    selector: 'winery-entity',
    templateUrl: './entity.component.html',
    styleUrls: ['./entityContainer.component.css'],
    providers: [
        EntityService
    ]
})
export class EntityComponent implements OnInit {

    @Input() data: SectionData;
    @Input() toscaType: ToscaTypes;
    @Input() xsdSchemaType: string;
    @Input() maxWidth: number;
    @Output() deleted = new EventEmitter<string>();

    @ViewChild('targetAllocationModal') targetAllocationModal: TargetAllocationComponent;
    @ViewChild('confirmDeleteModal') confirmDeleteModal: ModalDirective;

    imageUrl: string;
    backendLink: string;
    editButtonToolTip = 'Edit.';
    showButtons = true;
    showTargetAllocationButton: boolean;
    containerSizeClass: string;

    constructor(private existService: ExistService, private router: Router,
                private service: EntityService, private notify: WineryNotificationService,
                private configurationService: WineryRepositoryConfigurationService) {
    }

    ngOnInit(): void {
        this.backendLink = backendBaseURL + '/' + this.toscaType + '/'
            + encodeURIComponent(encodeURIComponent(this.data.namespace)) + '/' + this.data.id;

        if (this.toscaType === ToscaTypes.NodeType && this.data.id) {
            const img = this.backendLink + '/appearance/50x50';

            this.existService.check(img)
                .subscribe(
                    () => {
                        this.imageUrl = img;
                    },
                    () => {
                        this.imageUrl = null;
                    },
                );
        }

        if (this.toscaType === ToscaTypes.ServiceTemplate) {
            this.editButtonToolTip += ' Hold CTRL to directly edit the topology template.';
        }

        this.showButtons = this.toscaType !== ToscaTypes.Imports;
        this.showTargetAllocationButton = !this.configurationService.isYaml() && this.toscaType === ToscaTypes.ServiceTemplate;

        if (this.maxWidth === 380) {
            this.containerSizeClass = 'smallContainer';
        } else if (this.maxWidth === 440) {
            this.containerSizeClass = 'middleContainerSize';
        } else {
            this.containerSizeClass = 'largeContainer';
        }
    }

    onClick(event: MouseEvent) {
        let url = '/' + this.toscaType + '/';
        if (this.toscaType === ToscaTypes.Imports) {
            url += encodeURIComponent(encodeURIComponent(this.xsdSchemaType))
                + '/' + encodeURIComponent(encodeURIComponent(this.data.namespace));
        } else {
            url += encodeURIComponent(encodeURIComponent(this.data.namespace));
        }
        if (this.data.id) {
            url += '/' + this.data.id;
        }

        if (event.ctrlKey) {
            url = window.location.origin + window.location.pathname + '#' + url;
            window.open(url, '_blank');
        } else {
            this.router.navigateByUrl(url);
        }
    }

    targetAllocation(event: MouseEvent) {
        event.stopPropagation();
        this.targetAllocationModal.showModal(this.backendLink);
    }

    exportComponent(event: MouseEvent) {
        event.stopPropagation();
        if (event.ctrlKey) {
            if (!this.configurationService.isYaml()) {
                window.open(this.backendLink + '?xml', '_blank');
            } else {
                window.open(this.backendLink + '?yaml', '_blank');
            }
        } else {
            if (!this.configurationService.isYaml()) {
                window.open(this.backendLink + '?csar', '_blank');
            } else {
                window.open(this.backendLink + '?yaml&csar', '_blank');
            }
        }
    }

    editComponent(event: MouseEvent) {
        event.stopPropagation();
        if (this.toscaType === ToscaTypes.ServiceTemplate && event.ctrlKey) {
            const topologyModeler = this.configurationService.configuration.endpoints.topologymodeler
                + '?repositoryURL=' + encodeURIComponent(backendBaseURL)
                + '&uiURL=' + encodeURIComponent(window.location.origin + window.location.pathname)
                + '&ns=' + encodeURIComponent(this.data.namespace)
                + '&id=' + this.data.id;
            window.open(topologyModeler, '_blank');
        } else {
            this.router.navigateByUrl('/' + this.toscaType + '/' +
                encodeURIComponent(encodeURIComponent(this.data.namespace)) + '/'
                + this.data.id);
        }
    }

    showRemoveDialog(event: MouseEvent) {
        this.confirmDeleteModal.show();
        event.stopPropagation();
    }

    deleteConfirmed() {
        this.service.deleteComponent(this.backendLink)
            .subscribe(
                data => this.success(),
                error => this.notify.error('Error deleting ' + this.data.id)
            );
    }

    private success() {
        this.notify.success('Successfully deleted ' + this.data.id);
        this.deleted.emit(this.data.id);
    }
}
