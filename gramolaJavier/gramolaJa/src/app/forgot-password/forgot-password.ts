import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css'
})
export class ForgotPasswordComponent {
  private http = inject(HttpClient);
  private router = inject(Router);
  private backendUrl = 'http://127.0.0.1:8080';

  email: string = '';
  isLoading: boolean = false;
  success: boolean = false;
  errorMessage: string = '';

  requestReset() {
    if (!this.email) {
      this.errorMessage = 'Por favor ingresa tu correo electrónico';
      return;
    }

    if (!this.isValidEmail(this.email)) {
      this.errorMessage = 'Por favor ingresa un correo electrónico válido';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.http.post(`${this.backendUrl}/user/requestPasswordReset`, 
      { email: this.email },
      { withCredentials: true, responseType: 'text' }
    ).subscribe({
      next: (response) => {
        console.log('✅ Email de recuperación enviado');
        this.success = true;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('❌ Error:', err);
        this.isLoading = false;
        
        if (err.status === 404) {
          this.errorMessage = 'No existe una cuenta con ese correo electrónico';
        } else {
          this.errorMessage = 'Error al enviar el correo de recuperación. Inténtalo de nuevo.';
        }
      }
    });
  }

  private isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  goToLogin() {
    this.router.navigate(['/login']);
  }
}
