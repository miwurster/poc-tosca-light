import { Component, OnInit } from '@angular/core';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import {products} from '../../serviceTemplates';


@Component({
  selector: 'winery-side-bar',
  templateUrl: './side-bar.component.html',
  styleUrls: ['./side-bar.component.css']
})
export class SideBarComponent implements OnInit {
    products = products;
  constructor() { }

  ngOnInit() {
  }
    /*
    drop(event: CdkDragDrop<string[]>) {
        moveItemInArray(this.products, event.previousIndex, event.currentIndex);
    }
    */
}
