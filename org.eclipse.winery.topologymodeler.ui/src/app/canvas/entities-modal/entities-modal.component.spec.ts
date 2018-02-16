import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { EntitiesModalComponent } from './entities-modal.component';

describe('EntitiesModalComponent', () => {
  let component: EntitiesModalComponent;
  let fixture: ComponentFixture<EntitiesModalComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ EntitiesModalComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EntitiesModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
