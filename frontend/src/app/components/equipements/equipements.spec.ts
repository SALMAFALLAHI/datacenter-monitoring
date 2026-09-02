import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Equipements } from './equipements';

describe('Equipements', () => {
  let component: Equipements;
  let fixture: ComponentFixture<Equipements>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Equipements],
    }).compileComponents();

    fixture = TestBed.createComponent(Equipements);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
