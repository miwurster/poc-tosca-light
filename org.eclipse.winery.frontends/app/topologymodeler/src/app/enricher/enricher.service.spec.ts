import { TestBed } from '@angular/core/testing';

import { EnricherService } from './enricher.service';

describe('EnricherService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: EnricherService = TestBed.get(EnricherService);
    expect(service).toBeTruthy();
  });
});
