import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

declare var Stripe: any;

@Component({
  selector: 'app-queue-payment',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './queue-payment.html',
  styleUrl: './queue-payment.css'
})
export class QueuePaymentComponent implements OnInit {
  @Input() track: any;
  @Input() deviceId: string = '';
  @Output() close = new EventEmitter<void>();
  
  private backendUrl = 'http://127.0.0.1:8080';
  private stripe = Stripe("pk_test_51RJHbY00mVaZlVqdS1kfaMj7rlz9TTJsPIdv8YFu4qXTRdeCvr0Qmt3WfC3r0AmPNcgPcs8q3Y5p1kjRXPZZOOXk00SxO09d8M");
  
  email: string = '';
  selectedPrice: number = 199; // Standard por defecto
  loading: boolean = false;
  showStripeForm: boolean = false;
  clientSecret: string = '';
  
  private cardElement: any;
  
  constructor(private http: HttpClient) {}
  
  ngOnInit() {
    this.email = sessionStorage.getItem('userEmail') || '';
    console.log('💳 Queue Payment Modal abierto');
    console.log('🎵 Track:', this.track?.name);
    console.log('📱 Device:', this.deviceId);
  }
  
  async processPayment() {
    console.log('═══════════════════════════════════');
    console.log('💳 PROCESANDO PAGO DE ENCOLAMIENTO');
    console.log('═══════════════════════════════════');
    
    if (!this.track || !this.deviceId) {
      alert('⚠️ Faltan datos de la canción o dispositivo');
      return;
    }
    
    const accessToken = sessionStorage.getItem('spotify_access_token');
    if (!accessToken) {
      alert('⚠️ No hay token de Spotify');
      return;
    }
    
    this.loading = true;
    
    const payload = {
      email: this.email,
      accessToken: accessToken,
      trackUri: this.track.uri,
      deviceId: this.deviceId,
      priceCode: this.selectedPrice
    };
    
    console.log('📦 Payload:', payload);
    
    try {
      // 1. Preparar pago
      const clientSecret = await this.http.post<string>(
        `${this.backendUrl}/queue/prepay`,
        payload,
        { withCredentials: true }
      ).toPromise();
      
      if (!clientSecret) {
        throw new Error('No se recibió client secret');
      }
      
      this.clientSecret = clientSecret;
      console.log('✅ Client Secret recibido');
      
      // 2. Mostrar formulario de Stripe
      this.showStripeForm = true;
      this.loading = false;
      
      setTimeout(() => this.initStripeForm(), 100);
      
    } catch (error: any) {
      this.loading = false;
      console.error('❌ Error:', error);
      alert('❌ Error al preparar el pago:\n\n' + 
            (error.error?.message || error.message));
    }
  }
  
  private initStripeForm() {
    console.log('🎨 Inicializando formulario de Stripe');
    
    const elements = this.stripe.elements();
    
    this.cardElement = elements.create('card', {
      style: {
        base: {
          fontSize: '16px',
          color: '#32325d',
          '::placeholder': { color: '#aab7c4' }
        },
        invalid: { color: '#fa755a' }
      }
    });
    
    this.cardElement.mount('#queue-card-element');
    
    this.cardElement.on('change', (event: any) => {
      const errorElement = document.getElementById('queue-card-error');
      if (errorElement) {
        errorElement.textContent = event.error ? event.error.message : '';
      }
    });
  }
  
  async confirmPayment() {
    console.log('✅ Confirmando pago con Stripe');
    this.loading = true;
    
    try {
      const result = await this.stripe.confirmCardPayment(this.clientSecret, {
        payment_method: { card: this.cardElement }
      });
      
      if (result.error) {
        throw new Error(result.error.message);
      }
      
      console.log('✅ Pago exitoso en Stripe');
      console.log('🎵 Encolando canción...');
      
      // Confirmar en backend
      await this.http.get(
        `${this.backendUrl}/queue/confirm?deviceId=${this.deviceId}`,
        { withCredentials: true, responseType: 'text' }
      ).toPromise();
      
      console.log('✅ Canción encolada exitosamente');
      
      alert(`✅ ¡Pago Exitoso!\n\n"${this.track.name}" ha sido añadida a la cola`);
      
      this.closeModal();
      
    } catch (error: any) {
      console.error('❌ Error:', error);
      alert('❌ Error:\n\n' + (error.error || error.message));
    } finally {
      this.loading = false;
    }
  }
  
  closeModal() {
    this.close.emit();
  }
}