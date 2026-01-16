import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

declare let Stripe: any;

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payment.html',
  styleUrl: './payment.css'
})
export class PaymentComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private http = inject(HttpClient);
  
  // ⚠️ CAMBIA ESTO POR TU PUBLIC KEY DE STRIPE
  stripe = new Stripe("pk_test_51RJHbY00mVaZlVqdS1kfaMj7rlz9TTJsPIdv8YFu4qXTRdeCvr0Qmt3WfC3r0AmPNcgPcs8q3Y5p1kjRXPZZOOXk00SxO09d8M");
  
  transactionDetails: any;
  token: string = '';
  email: string = '';
  loading: boolean = false;
  selectedSubscription: string = 'monthly'; // 'monthly' o 'annual'
  prices: any = {
    monthly: { amount: 29.99, label: 'Mensual' },
    annual: { amount: 299, label: 'Anual' }
  };

  ngOnInit(): void {
    // ✅ Obtener AMBOS parámetros de la URL
    this.route.queryParams.subscribe(params => {
      this.email = params['email'] || '';
      this.token = params['token'] || '';
      
      console.log('📧 Email:', this.email);
      console.log('🔑 Token:', this.token);
      
      if (!this.email || !this.token) {
        alert('⚠️ Acceso no autorizado.\n\nDebes confirmar tu cuenta primero.');
        this.router.navigate(['/register']);
      }
    });
    
    // Cargar precios desde BD
    this.loadPrices();
  }
  
  loadPrices() {
    this.http.get('http://127.0.0.1:8080/payments/prices', {
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log('💰 Precios desde BD:', response);
        
        // Mapear precios si vienen de BD
        if (Array.isArray(response) && response.length > 0) {
          response.forEach((price: any) => {
            const euros = price.priceCents / 100;
            if (price.description?.toLowerCase().includes('mensual')) {
              this.prices.monthly.amount = euros;
            } else if (price.description?.toLowerCase().includes('anual')) {
              this.prices.annual.amount = euros;
            }
          });
        }
      },
      error: (err) => {
        console.warn('⚠️ No se pudieron cargar precios de BD, usando valores por defecto:', err);
      }
    });
  }

    prepay() {
      this.loading = true;
      
      this.http.get(`http://127.0.0.1:8080/payments/prepay?subscriptionType=${this.selectedSubscription}`, {
        withCredentials: true 
      }).subscribe({
      next: (response: any) => {
        this.loading = false;
        this.transactionDetails = response;
        console.log('💳 Transaction details:', this.transactionDetails);
        this.showForm();
      },
      error: (err) => {
        this.loading = false;
        alert('❌ Error al preparar el pago:\n\n' + 
              (err.error?.message || err.message || 'Error desconocido'));
        console.error('Error completo:', err);
      }
    });
  }

  showForm() {
    let elements = this.stripe.elements();
    
    let style = {
      base: {
        color: "#32325d",
        fontFamily: 'Arial, sans-serif',
        fontSmoothing: "antialiased",
        fontSize: "16px",
        "::placeholder": {
          color: "#32325d"
        }
      },
      invalid: {
        fontFamily: 'Arial, sans-serif',
        color: "#fa755a",
        iconColor: "#fa755a"
      }
    };
    
    let card = elements.create("card", { 
      style: style,
      hidePostalCode: true
    });
    
    card.mount("#card-element");
    
    card.on("change", (event: any) => {
      const submitButton = document.querySelector("button#submit-payment") as HTMLButtonElement;
      if (submitButton) {
        submitButton.disabled = event.empty;
      }
      
      const errorElement = document.querySelector("#card-error");
      if (errorElement) {
        errorElement.textContent = event.error ? event.error.message : "";
      }
    });
    
    let self = this;
    let form = document.getElementById("payment-form");
    
    if (form) {
      form.addEventListener("submit", function (event) {
        event.preventDefault();
        self.payWithCard(card);
      });
      
      form.style.display = "block";
    }
  }

  payWithCard(card: any) {
    let self = this;
    
    const dataJson = JSON.parse(this.transactionDetails.data);
    const clientSecret = dataJson.client_secret;
    
    console.log('💳 Procesando pago con Stripe...');
    
    this.stripe.confirmCardPayment(clientSecret, {
      payment_method: {
        card: card
      }
    }).then(function (response: any) {
      if (response.error) {
        alert('❌ Error en el pago:\n\n' + response.error.message);
        console.error('Error de Stripe:', response.error);
      } else {
        if (response.paymentIntent.status === 'succeeded') {
          console.log('✅ Pago exitoso en Stripe');
          
          // ✅ Enviar EMAIL, TOKEN y SUBSCRIPTION TYPE al backend
          self.http.post('http://127.0.0.1:8080/payments/confirm', {
            paymentIntent: response.paymentIntent,
            transactionId: self.transactionDetails.id,
            token: self.token,
            email: self.email,
            subscriptionType: self.selectedSubscription
          }, { 
            withCredentials: true 
          }).subscribe({
            next: () => {
              const subscriptionLabel = self.selectedSubscription === 'annual' ? 'anual' : 'mensual';
              alert('✅ ¡Pago Exitoso!\n\n' +
                    'Tu cuenta ha sido activada.\n' +
                    'Suscripción: ' + subscriptionLabel + '\n\n' +
                    'Ya puedes iniciar sesión y disfrutar de tu gramola.');
              
              self.router.navigate(['/login'], {
                queryParams: { 
                  message: '¡Pago confirmado! Ya puedes iniciar sesión' 
                }
              });
            },
            error: (err) => {
              alert('❌ Error al confirmar el pago:\n\n' + 
                    (err.error?.message || err.message || 'Error desconocido'));
              console.error('Error al confirmar:', err);
            }
          });
        }
      }
    });
  }

  goToRegister() {
    this.router.navigate(['/register']);
  }
}