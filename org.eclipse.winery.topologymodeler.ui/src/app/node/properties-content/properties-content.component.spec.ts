import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { PropertiesContentComponent } from './properties-content.component';

describe('PropertiesContentComponent', () => {
  let component: PropertiesContentComponent;
  let fixture: ComponentFixture<PropertiesContentComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ PropertiesContentComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PropertiesContentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
