import { Component, OnInit } from '@angular/core';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import {products} from '../../serviceTemplates';
import {HttpServiceTemplates} from '../../services/httpClient';

@Component({
  selector: 'winery-side-bar',
  templateUrl: './side-bar.component.html',
  styleUrls: ['./side-bar.component.css']
})
export class SideBarComponent implements OnInit {
    products = products;
    serviceTemplates: any;
  constructor(private httpServiceTemplates: HttpServiceTemplates) { }

  ngOnInit() {
      this.httpServiceTemplates.getServiceTemplates().subscribe((data: any) => {
          console.log(data);
          this.serviceTemplates = data['ServiceTemplate'];
      });
  }
    /*
    drop(event: CdkDragDrop<string[]>) {
        moveItemInArray(this.products, event.previousIndex, event.currentIndex);
    }
    */
}
