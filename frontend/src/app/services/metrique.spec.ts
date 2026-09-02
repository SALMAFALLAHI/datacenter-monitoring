import { TestBed } from '@angular/core/testing';

import { Metrique } from './metrique';

describe('Metrique', () => {
  let service: Metrique;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Metrique);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
