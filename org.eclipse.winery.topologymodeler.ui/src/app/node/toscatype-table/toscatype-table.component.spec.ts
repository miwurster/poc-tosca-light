import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ToscatypeTableComponent } from './toscatype-table.component';

describe('ToscatypeTableComponent', () => {
  let component: ToscatypeTableComponent;
  let fixture: ComponentFixture<ToscatypeTableComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ToscatypeTableComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ToscatypeTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
