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
  @Input() initialPrice: number = 299; // Precio inicial desde el componente padre
  @Output() close = new EventEmitter<void>();
  
  private backendUrl = 'http://127.0.0.1:8080';
  private stripe = Stripe("pk_test_51RJHbY00mVaZlVqdS1kfaMj7rlz9TTJsPIdv8YFu4qXTRdeCvr0Qmt3WfC3r0AmPNcgPcs8q3Y5p1kjRXPZZOOXk00SxO09d8M");
  
  customerName: string = '';  // Nombre de quien paga
  selectedPrice: number = 299;
  loading: boolean = false;
  showStripeForm: boolean = false;
  clientSecret: string = '';
  
  private cardElement: any;
  
  constructor(private http: HttpClient) {}
  
  ngOnInit() {
    this.selectedPrice = this.initialPrice;
    console.log('💳 Queue Payment Modal abierto');
    console.log('🎵 Track:', this.track?.name);
    console.log('📱 Device:', this.deviceId);
    console.log('💰 Precio inicial:', this.selectedPrice === 199 ? '1.99€ (Adelantar)' : '2.99€ (Nueva)');
  }
  
  async processPayment() {
    console.log('═══════════════════════════════════');
    console.log('💳 PROCESANDO PAGO DE ENCOLAMIENTO');
    console.log('═══════════════════════════════════');
    
    if (!this.track || !this.deviceId) {
      alert('⚠️ Faltan datos de la canción o dispositivo');
      return;
    }
    
    if (!this.customerName || this.customerName.trim().length < 2) {
      alert('⚠️ Por favor ingresa tu nombre');
      return;
    }
    
    const accessToken = sessionStorage.getItem('spotify_access_token');
    if (!accessToken) {
      alert('⚠️ No hay token de Spotify');
      return;
    }
    
    this.loading = true;
    
    // Obtener ubicación del usuario
    try {
      const position = await this.getUserLocation();
      
      const payload = {
        customerName: this.customerName.trim(),
        accessToken: accessToken,
        trackUri: this.track.uri,
        trackName: this.track.name,
        artistName: this.track.artists[0]?.name || 'Unknown',
        albumName: this.track.album?.name || '',
        deviceId: this.deviceId,
        priceCode: this.selectedPrice,
        userLatitude: position.coords.latitude,
        userLongitude: position.coords.longitude
      };
      
      console.log('📍 Ubicación del usuario:', position.coords.latitude, position.coords.longitude);
      console.log('📦 Payload:', payload);
    
      // 1. Preparar pago
      const clientSecret = await this.http.post(
        `${this.backendUrl}/queue/prepay`,
        payload,
        { 
          withCredentials: true,
          responseType: 'text'
        }
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
      
      // Verificar si es error de geolocalización
      if (error.message?.includes('ubicación') || error.message?.includes('Permiso')) {
        alert('⚠️ Debes permitir el acceso a tu ubicación para usar la gramola.\n\n' +
              'La aplicación solo funciona dentro del bar (radio de 100 metros).');
      } else {
        alert('❌ Error al preparar el pago:\n\n' + 
              (error.error?.message || error.message));
      }
    }
  }
  
  private initStripeForm() {
    console.log('🎨 Inicializando formulario de Stripe');
    
    const elements = this.stripe.elements();
    
    this.cardElement = elements.create('card', {
      hidePostalCode: true, // Ocultar código postal
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
  
  /**
   * Obtiene la ubicación actual del usuario
   */
  private getUserLocation(): Promise<GeolocationPosition> {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error('Geolocalización no disponible en este navegador'));
        return;
      }
      
      navigator.geolocation.getCurrentPosition(
        (position) => resolve(position),
        (error) => {
          console.error('Error obteniendo ubicación:', error);
          let errorMessage = 'Error obteniendo ubicación';
          
          switch(error.code) {
            case error.PERMISSION_DENIED:
              errorMessage = 'Permiso de ubicación denegado';
              break;
            case error.POSITION_UNAVAILABLE:
              errorMessage = 'Ubicación no disponible';
              break;
            case error.TIMEOUT:
              errorMessage = 'Tiempo de espera agotado';
              break;
          }
          
          reject(new Error(errorMessage));
        },
        {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 0
        }
      );
    });
  }
}