import { ComponentFixture, TestBed } from '@angular/core/testing';

import { QueuePayment } from './queue-payment';

describe('QueuePayment', () => {
  let component: QueuePayment;
  let fixture: ComponentFixture<QueuePayment>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QueuePayment]
    })
    .compileComponents();

    fixture = TestBed.createComponent(QueuePayment);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
