import { TestBed } from '@angular/core/testing';

import { Equipement } from './equipement';

describe('Equipement', () => {
  let service: Equipement;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Equipement);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
