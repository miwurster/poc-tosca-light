import { TestBed, inject } from '@angular/core/testing';

import { ExistsService } from './exists.service';

describe('ExistsService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ExistsService]
    });
  });

  it('should be created', inject([ExistsService], (service: ExistsService) => {
    expect(service).toBeTruthy();
  }));
});
