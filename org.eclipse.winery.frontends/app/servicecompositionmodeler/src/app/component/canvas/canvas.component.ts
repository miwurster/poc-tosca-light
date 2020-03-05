import { AfterViewInit, Component, OnInit, ViewChild , ElementRef} from '@angular/core';
import {JsPlumbService} from '../../services/jsPlumb.service';
import {jsPlumb, jsPlumbInstance } from 'jsplumb';


@Component({
  selector: 'winery-canvas',
  templateUrl: './canvas.component.html',
  styleUrls: ['./canvas.component.css']
})
export class CanvasComponent implements OnInit, AfterViewInit {
    @ViewChild('dragDropWindow1') myId: ElementRef;
    @ViewChild('dragDropWindow2') myIdtwo: ElementRef;
    plumbInstance: any;
    constructor(jsPlumbService: JsPlumbService) {
      // this.plumbInstance = jsPlumbService.getJsPlumbInstance();
  }
  ngOnInit() {
      jsPlumb.getInstance().ready(() => {
          let plumbtest: any;
          plumbtest = jsPlumb.getInstance();
          plumbtest.setContainer(('.model-area'));
          plumbtest.importDefaults({
              Connector : [ 'Flowchart', { curviness: 150 } ],
              Anchors : [ 'RightMiddle', 'LeftMiddle' ],
              Container: 'canvas',
          });
          const e1 = plumbtest.addEndpoint('dragDropWindow1', { uuid: 'test1' });
          const e2 = plumbtest.addEndpoint('dragDropWindow2', { uuid: 'test2' });
          const e3 = plumbtest.addEndpoint('dragDropWindow3', { uuid: 'test3' });
          const e4 = plumbtest.addEndpoint('dragDropWindow4');
          plumbtest.draggable(jsPlumb.getInstance().getSelector(('.window')));
          plumbtest.connect({ uuids: ['test1', 'test3'] });
          plumbtest.connect({
              source: 'dragDropWindow1',
              target: 'dragDropWindow2',
              overlays: [
                  'Arrow',
                  [ 'Label', { label: 'serviceTemplate', location: 0.25, id: 'myLabel' } ]
              ],
          });
          plumbtest.connect({
              source: 'dragDropWindow2',
              target: 'dragDropWindow4',
          });
      });
  }
  /* createTestConnection() {
      const e1 = this.plumbInstance.addEndpoint( 'dragDropWindow1');
      const e2  = this.plumbInstance.addEndpoint( 'dragDropWindow1');
      this.plumbInstance.draggable(('.window'));
      this.plumbInstance.connect({
          source: 'e1',
          target: 'e2',
      });
  } */

    ngAfterViewInit(): void {
      console.log(document.getElementById('dragDropWindow1'));
        console.log(this.myId.nativeElement.innerHTML);
        console.log(this.myIdtwo.nativeElement.innerHTML);
    }
}
