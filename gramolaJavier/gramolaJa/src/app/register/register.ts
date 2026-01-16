import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UserService } from '../user';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {
  private userService = inject(UserService);
  private router = inject(Router);
  
  barName: string = '';
  email: string = '';
  pwd1: string = '';
  pwd2: string = '';
  clientId: string = '';
  clientSecret: string = '';
  isLoading: boolean = false;

  register() {
    // Validaciones del lado del cliente
    if (!this.barName || !this.email || !this.pwd1 || !this.pwd2 || 
        !this.clientId || !this.clientSecret) {
      alert('⚠️ Por favor complete todos los campos obligatorios');
      return;
    }
    
    if (this.pwd1 !== this.pwd2) {
      alert('⚠️ Las contraseñas no coinciden');
      return;
    }
    
    if (!this.isValidEmail(this.email)) {
      alert('⚠️ Por favor ingrese un correo electrónico válido');
      return;
    }
    
    if (this.pwd1.length < 6) {
      alert('⚠️ La contraseña debe tener al menos 6 caracteres');
      return;
    }
    
    if (this.barName.trim().length < 3) {
      alert('⚠️ El nombre del bar debe tener al menos 3 caracteres');
      return;
    }
    
    // Registro
    this.isLoading = true;
    
    this.userService.register(
      this.email, 
      this.pwd1, 
      this.pwd2, 
      this.barName, 
      this.clientId, 
      this.clientSecret
    ).subscribe({
      next: () => {
        // ✅ Solo recibimos 200 OK (void)
        // NO se muestra el token aquí
        this.isLoading = false;
        
        console.log('✅ Registro exitoso para:', this.email);
        
        // Mostrar mensaje GENÉRICO sin mencionar el token
        alert(
          '✅ ¡Registro Exitoso!\n\n' +
          'Hemos enviado un correo de confirmación a:\n' +
          this.email + '\n\n' +
          '📧 Por favor, revisa tu bandeja de entrada (y spam)\n' +
          'y haz clic en el enlace de confirmación.\n\n' +
          '⏱️ El enlace expirará en 30 minutos.\n\n' +
          '(En desarrollo, el correo aparece en la consola del servidor)'
        );
        
        // Limpiar formulario
        this.clearForm();
        
        // Redirigir al login después de 5 segundos
        setTimeout(() => {
          this.router.navigate(['/login'], {
            queryParams: { 
              message: 'Por favor confirma tu cuenta antes de iniciar sesión' 
            }
          });
        }, 5000);
      },
      error: (err) => {
        this.isLoading = false;
        
        // Mensajes de error personalizados según código HTTP
        let errorMessage = 'Error desconocido';
        let shouldRedirectToLogin = false;
        
        if (err.status === 409) {
          // Usuario ya existe y está activo
          errorMessage = 
            '⚠️ Este bar ya está registrado y activo.\n\n' +
            '¿Deseas iniciar sesión?';
          shouldRedirectToLogin = true;
          
        } else if (err.status === 406) {
          // Datos inválidos
          errorMessage = err.error?.message || 
            'Datos inválidos.\n\n' +
            'Por favor verifica:\n' +
            '• Las contraseñas coinciden\n' +
            '• El email es válido\n' +
            '• Todos los campos están completos';
          
        } else if (err.status === 0) {
          // Error de red
          errorMessage = 
            '❌ Error de conexión.\n\n' +
            'No se pudo conectar con el servidor.\n' +
            'Verifica que el backend esté corriendo en:\n' +
            'http://localhost:8080';
          
        } else {
          errorMessage = err.error?.message || err.message || 
            'Error al registrar el bar.\n\nInténtalo nuevamente.';
        }
        
        alert('❌ Error en el registro:\n\n' + errorMessage);
        console.error('Error completo:', err);
        
        // Redirigir a login si el usuario ya existe
        if (shouldRedirectToLogin && confirm(errorMessage)) {
          this.router.navigate(['/login']);
        }
      }
    });
  }
  
  private isValidEmail(email: string): boolean {
    const emailRegex = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
    return emailRegex.test(email);
  }
  
  private clearForm() {
    this.barName = '';
    this.email = '';
    this.pwd1 = '';
    this.pwd2 = '';
    this.clientId = '';
    this.clientSecret = '';
  }
}