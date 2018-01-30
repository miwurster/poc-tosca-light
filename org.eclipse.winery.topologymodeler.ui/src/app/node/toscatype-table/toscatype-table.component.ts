import { Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { IWineryState } from '../../redux/store/winery.store';
import { NgRedux } from '@angular-redux/store';
import { TNodeTemplate } from '../../models/ttopology-template';
import { QName } from '../../qname';
import { isNullOrUndefined } from 'util';

@Component({
    selector: 'winery-toscatype-table',
    templateUrl: './toscatype-table.component.html',
    styleUrls: ['./toscatype-table.component.css']
})
export class ToscatypeTableComponent implements OnInit, OnChanges {

    @Input() toscaType: string;
    @Input() currentNodeData: any;
    @Input() toscaTypeData: any;
    currentToscaTypeData;
    currentToscaType;
    latestNodeTemplate?: any = {};

    constructor(private $ngRedux: NgRedux<IWineryState>) {
    }

    ngOnInit() {
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes['toscaTypeData']) {
            this.currentToscaTypeData = this.toscaTypeData;
            console.log(changes);
        }
        if (changes['toscaType']) {
            this.currentToscaType = this.toscaType;
        }
    }

    getLocalName(qName?: string): string {
        const qNameVar = new QName(qName);
        return qNameVar.localName;
    }

}
