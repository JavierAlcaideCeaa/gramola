import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.css'
})
export class ResetPasswordComponent implements OnInit {
  private http = inject(HttpClient);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private backendUrl = 'http://127.0.0.1:8080';

  email: string = '';
  token: string = '';
  newPassword: string = '';
  confirmPassword: string = '';
  isLoading: boolean = false;
  success: boolean = false;
  errorMessage: string = '';

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.email = params['email'] || '';
      this.token = params['token'] || '';

      if (!this.email || !this.token) {
        this.errorMessage = 'Enlace inválido o expirado';
      }
    });
  }

  resetPassword() {
    if (!this.newPassword || !this.confirmPassword) {
      this.errorMessage = 'Por favor completa todos los campos';
      return;
    }

    if (this.newPassword.length < 6) {
      this.errorMessage = 'La contraseña debe tener al menos 6 caracteres';
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'Las contraseñas no coinciden';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.http.post(`${this.backendUrl}/user/resetPassword`, 
      { 
        email: this.email,
        token: this.token,
        newPassword: this.newPassword
      },
      { withCredentials: true, responseType: 'text' }
    ).subscribe({
      next: (response) => {
        console.log('✅ Contraseña restablecida');
        this.success = true;
        this.isLoading = false;
        
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 3000);
      },
      error: (err) => {
        console.error('❌ Error:', err);
        this.isLoading = false;
        
        if (err.status === 404) {
          this.errorMessage = 'Token inválido o expirado';
        } else {
          this.errorMessage = 'Error al restablecer la contraseña. Inténtalo de nuevo.';
        }
      }
    });
  }

  goToLogin() {
    this.router.navigate(['/login']);
  }
}
