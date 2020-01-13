import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'winery-top-bar',
  templateUrl: './top-bar.component.html',
  styleUrls: ['./top-bar.component.css']
})
export class TopBarComponent implements OnInit {
    public save() {
        window.alert('post with source and target service template');
    }
  constructor() { }

  ngOnInit() {
  }

}
