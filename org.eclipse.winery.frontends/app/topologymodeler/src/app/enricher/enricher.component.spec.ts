import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { EnricherComponent } from './enricher.component';

describe('EnricherComponent', () => {
  let component: EnricherComponent;
  let fixture: ComponentFixture<EnricherComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ EnricherComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EnricherComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
