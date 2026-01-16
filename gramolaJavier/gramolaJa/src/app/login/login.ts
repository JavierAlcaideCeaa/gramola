import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { UserService, LoginResponse } from '../user';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink, CommonModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login implements OnInit {
  private userService = inject(UserService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  
  email: string = '';
  pwd: string = '';
  isLoading: boolean = false;
  infoMessage: string = '';

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      if (params['message']) {
        this.infoMessage = params['message'];
        setTimeout(() => {
          this.infoMessage = '';
        }, 10000);
      }
    });
  }

  login() {
    console.log('═══════════════════════════════════');
    console.log('🔐 INICIANDO LOGIN');
    console.log('═══════════════════════════════════');
    console.log('📧 Email:', this.email);
    console.log('═══════════════════════════════════');
    
    if (!this.email || !this.pwd) {
      alert('⚠️ Por favor complete todos los campos');
      return;
    }
    
    if (!this.isValidEmail(this.email)) {
      alert('⚠️ Por favor ingrese un correo electrónico válido');
      return;
    }
    
    if (this.pwd.length < 6) {
      alert('⚠️ La contraseña debe tener al menos 6 caracteres');
      return;
    }
    
    this.isLoading = true;
    
    this.userService.login(this.email, this.pwd).subscribe({
      next: (response: LoginResponse) => {
        this.isLoading = false;
        
        console.log('═══════════════════════════════════');
        console.log('✅ LOGIN EXITOSO');
        console.log('═══════════════════════════════════');
        console.log('📧 Email:', response.email);
        console.log('🏪 Bar:', response.barName);
        console.log('🎵 Client ID:', response.clientId);
        if (response.latitude && response.longitude) {
          console.log('📍 Coordenadas:', response.latitude, response.longitude);
        }
        console.log('═══════════════════════════════════');
        
        // Guardar datos en sessionStorage
        sessionStorage.setItem('userEmail', response.email);
        sessionStorage.setItem('barName', response.barName);
        sessionStorage.setItem('clientId', response.clientId);
        
        // Guardar coordenadas si están disponibles
        if (response.latitude && response.longitude) {
          sessionStorage.setItem('latitude', response.latitude);
          sessionStorage.setItem('longitude', response.longitude);
        }
        
        console.log('💾 Datos guardados en sessionStorage:');
        console.log('   userEmail:', sessionStorage.getItem('userEmail'));
        console.log('   barName:', sessionStorage.getItem('barName'));
        console.log('   clientId:', sessionStorage.getItem('clientId'));
        console.log('   latitude:', sessionStorage.getItem('latitude'));
        console.log('   longitude:', sessionStorage.getItem('longitude'));
        
        // Limpiar formulario
        this.clearForm();
        
        // ✅ INICIAR FLUJO OAUTH 2.0 CON SPOTIFY
        console.log('═══════════════════════════════════');
        console.log('🎵 INICIANDO FLUJO OAUTH 2.0');
        console.log('═══════════════════════════════════');
        
        this.initiateSpotifyAuth(response.clientId);
      },
      error: (err) => {
        this.isLoading = false;
        
        console.error('═══════════════════════════════════');
        console.error('❌ ERROR EN EL LOGIN');
        console.error('═══════════════════════════════════');
        console.error('🔴 Status:', err.status);
        console.error('🔴 Message:', err.message);
        console.error('🔴 Error completo:', err);
        console.error('═══════════════════════════════════');
        
        this.handleLoginError(err);
      }
    });
  }

  /**
   * ✅ Genera un string aleatorio para el state
   */
  private generateRandomString(length: number = 16): string {
    const possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let text = '';
    for (let i = 0; i < length; i++) {
      text += possible.charAt(Math.floor(Math.random() * possible.length));
    }
    return text;
  }

  /**
   * ✅ Inicia el flujo OAuth 2.0
   */
  private initiateSpotifyAuth(clientId: string) {
    console.log('🔐 Preparando autorización de Spotify...');
    console.log('🎵 Client ID:', clientId);
    
    // Scopes
    const scopes = [
      "user-read-private",
      "user-read-email",
      "playlist-read-private",
      "playlist-read-collaborative",
      "user-read-playback-state",
      "user-modify-playback-state",
      "user-read-currently-playing",
      "user-library-read",
      "user-library-modify",
      "user-read-recently-played",
      "user-top-read",
      "app-remote-control",
      "streaming"
    ];
    
    console.log('📋 Scopes configurados:', scopes.length, 'permisos');
    
    // Generar state aleatorio
    const state = this.generateRandomString();
    console.log('🔑 State generado:', state);
    
    // Guardar state en sessionStorage
    sessionStorage.setItem('oauth_state', state);
    console.log('💾 State guardado en sessionStorage');
    
    const redirectUri = 'http://127.0.0.1:4200/callback';
    console.log('🔗 Redirect URI:', redirectUri);
    
    // Construir URL
    let params = "response_type=code";
    params += `&client_id=${encodeURIComponent(clientId)}`;
    params += `&scope=${encodeURIComponent(scopes.join(" "))}`;
    params += `&redirect_uri=${encodeURIComponent(redirectUri)}`;
    params += `&state=${encodeURIComponent(state)}`;
    
    const authUrl = `https://accounts.spotify.com/authorize?${params}`;
    
    console.log('═══════════════════════════════════');
    console.log('🚀 REDIRIGIENDO A SPOTIFY');
    console.log('═══════════════════════════════════');
    console.log('🔗 URL completa (primeros 100 chars):');
    console.log(authUrl.substring(0, 100) + '...');
    console.log('═══════════════════════════════════');
    console.log('⏱️ Redirigiendo en 2 segundos...');
    console.log('═══════════════════════════════════');
    
    // ✅ AÑADIR DELAY PARA VER LOGS
    setTimeout(() => {
      console.log('🌐 Ejecutando window.location.href...');
      window.location.href = authUrl;
    }, 2000); // 2 segundos de delay
  }

  private handleLoginError(err: any) {
    let errorMessage = 'Error desconocido';
    
    if (err.status === 404) {
      errorMessage = 
        '❌ El usuario no existe.\n\n' +
        '¿Necesitas registrarte?';
      
      if (confirm(errorMessage)) {
        this.router.navigate(['/register']);
      }
      return;
      
    } else if (err.status === 401) {
      errorMessage = 
        '❌ Contraseña incorrecta.\n\n' +
        'Por favor, inténtalo de nuevo.';
      
    } else if (err.status === 403) {
      errorMessage = 
        '⚠️ Debes confirmar tu cuenta.\n\n' +
        '📧 Revisa tu correo electrónico y haz clic\n' +
        'en el enlace de confirmación.\n\n' +
        '¿No recibiste el correo? Regístrate nuevamente.';
      
      if (confirm(errorMessage + '\n\n¿Ir a registro?')) {
        this.router.navigate(['/register']);
      }
      return;
      
    } else if (err.status === 402) {
      errorMessage = 
        '💳 Debes completar el pago.\n\n' +
        '¿Deseas proceder al pago ahora?';
      
      if (confirm(errorMessage)) {
        this.router.navigate(['/payment'], { 
          queryParams: { email: this.email } 
        });
      }
      return;
      
    } else {
      errorMessage = err.error?.message || err.message || 
        'Error al iniciar sesión';
    }
    
    alert('❌ Error en el login:\n\n' + errorMessage);
    console.error('Error completo:', err);
  }
  
  private isValidEmail(email: string): boolean {
    const emailRegex = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
    return emailRegex.test(email);
  }
  
  private clearForm() {
    this.email = '';
    this.pwd = '';
  }
}