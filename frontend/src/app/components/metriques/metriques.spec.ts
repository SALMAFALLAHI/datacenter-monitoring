import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Metriques } from './metriques';

describe('Metriques', () => {
  let component: Metriques;
  let fixture: ComponentFixture<Metriques>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Metriques],
    }).compileComponents();

    fixture = TestBed.createComponent(Metriques);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
