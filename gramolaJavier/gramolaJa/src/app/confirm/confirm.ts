import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-confirm',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './confirm.html',
  styleUrl: './confirm.css'
})
export class Confirm implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  
  loading: boolean = false;
  success: boolean = true;  // ✅ Siempre success porque el backend ya confirmó
  error: string = '';
  email: string = '';

  ngOnInit() {
    // ✅ El backend ya confirmó la cuenta y redirigió aquí
    // Solo obtenemos el email de la URL
    this.route.queryParams.subscribe(params => {
      this.email = params['email'] || '';
      
      if (!this.email) {
        this.error = 'Email no proporcionado';
        this.success = false;
        return;
      }
      
      console.log('✅ Cuenta confirmada para:', this.email);
      
      // Redirigir al pago después de 2 segundos
      setTimeout(() => {
        this.router.navigate(['/payment'], { 
          queryParams: params  // Pasar TODOS los params (email + token)
        });
      }, 2000);
    });
  }
  
  goToRegister() {
    this.router.navigate(['/register']);
  }
}