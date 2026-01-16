import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-callback',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './callback.html',
  styleUrl: './callback.css'
})
export class CallbackComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private http = inject(HttpClient);
  
  private backendUrl = 'http://127.0.0.1:8080'; 
  
  
  loading = true;
  success = false;
  error = '';
  debugInfo = '';
  showDebug = false;  // ❌ Debug desactivado

  ngOnInit() {
    console.log('╔══════════════════════════════════════╗');
    console.log('║  CALLBACK COMPONENT INICIALIZADO     ║');
    console.log('╚══════════════════════════════════════╝');
    
    this.debugInfo += '✅ Componente cargado\n';
    this.debugInfo += `📅 Timestamp: ${new Date().toISOString()}\n`;
    this.debugInfo += `🌐 URL actual: ${window.location.href}\n\n`;
    
    this.route.queryParams.subscribe(params => {
      console.log('📦 Query Params recibidos:', params);
      
      const code = params['code'];
      const state = params['state'];
      const error = params['error'];
      
      this.debugInfo += '📋 PARÁMETROS DE SPOTIFY:\n';
      this.debugInfo += `  code: ${code ? code.substring(0, 20) + '...' : 'undefined'}\n`;
      this.debugInfo += `  state: ${state || 'undefined'}\n`;
      this.debugInfo += `  error: ${error || 'undefined'}\n\n`;
      
      if (error) {
        this.error = 'Autorización cancelada por el usuario';
        this.loading = false;
        return;
      }
      
      if (!code || !state) {
        this.error = 'Faltan parámetros del callback de Spotify';
        this.loading = false;
        return;
      }
      
      // Verificar state
      const savedState = sessionStorage.getItem('oauth_state');
      this.debugInfo += '🔐 VERIFICACIÓN DE STATE:\n';
      this.debugInfo += `  Recibido: ${state}\n`;
      this.debugInfo += `  Guardado: ${savedState}\n`;
      
      if (state !== savedState) {
        this.error = 'Error de seguridad: state no coincide';
        this.debugInfo += `  ❌ NO COINCIDEN\n\n`;
        this.loading = false;
        return;
      }
      
      this.debugInfo += `  ✅ STATE VÁLIDO\n\n`;
      
      // Obtener email del sessionStorage
      const email = sessionStorage.getItem('userEmail');
      
      this.debugInfo += '📧 SESSION STORAGE:\n';
      this.debugInfo += `  userEmail: ${email || 'undefined'}\n`;
      this.debugInfo += `  barName: ${sessionStorage.getItem('barName') || 'undefined'}\n`;
      this.debugInfo += `  clientId: ${sessionStorage.getItem('clientId') || 'undefined'}\n\n`;
      
      if (!email) {
        this.error = 'No se encontró el email en la sesión';
        this.loading = false;
        return;
      }
      
      // ✅ HACER LA PETICIÓN CON MÚLTIPLES INTENTOS
      this.makeRequestWithRetry(code, email, 3);
    });
  }

  /**
   * ✅ Hacer petición con reintentos
   */
  private makeRequestWithRetry(code: string, email: string, attemptsLeft: number) {
    const attemptNumber = 4 - attemptsLeft;
    
    console.log(`╔══════════════════════════════════════╗`);
    console.log(`║  INTENTO ${attemptNumber} DE 3                   ║`);
    console.log(`╚══════════════════════════════════════╝`);
    
    this.debugInfo += `\n🔄 INTENTO ${attemptNumber}:\n`;
    
    // Construir URL
    const endpoint = '/spotify/getAuthorizationToken';
    const params = `code=${encodeURIComponent(code)}&email=${encodeURIComponent(email)}`;
    const url = `${this.backendUrl}${endpoint}?${params}`;
    
    this.debugInfo += `  URL: ${this.backendUrl}${endpoint}\n`;
    this.debugInfo += `  Params: code=XXX&email=${email}\n`;
    
    console.log('🔗 URL completa:', url);
    console.log('⏰ Iniciando petición...', new Date().toISOString());
    
    const startTime = Date.now();
    
    // ✅ PETICIÓN HTTP SIMPLE
    this.http.get<any>(url, {
      // Sin withCredentials para evitar problemas CORS
      // Sin headers extra
    }).subscribe({
      next: (response) => {
        const elapsed = Date.now() - startTime;
        
        console.log('╔══════════════════════════════════════╗');
        console.log('║  ✅ RESPUESTA EXITOSA                 ║');
        console.log('╚══════════════════════════════════════╝');
        console.log('⏱️  Tiempo:', elapsed + 'ms');
        console.log('📦 Response:', response);
        
        this.debugInfo += `  ✅ Éxito en ${elapsed}ms\n`;
        this.debugInfo += `  📦 Access Token: ${response?.access_token ? 'Recibido' : 'NO recibido'}\n`;
        
        if (!response?.access_token) {
          this.error = 'El servidor no devolvió el access token';
          this.loading = false;
          return;
        }
        
        // Guardar token
        sessionStorage.setItem('spotify_access_token', response.access_token);
        
        this.success = true;
        this.loading = false;
        
        console.log('✅ Token guardado en sessionStorage');
        console.log('🚀 Redirigiendo al dashboard...');
        
        setTimeout(() => {
          this.router.navigate(['/dashboard']);
        }, 2000);
      },
      error: (err: HttpErrorResponse) => {
        const elapsed = Date.now() - startTime;
        
        console.error('╔══════════════════════════════════════╗');
        console.error('║  ❌ ERROR EN LA PETICIÓN              ║');
        console.error('╚══════════════════════════════════════╝');
        console.error('⏱️  Tiempo:', elapsed + 'ms');
        console.error('🔴 Status:', err.status);
        console.error('🔴 Status Text:', err.statusText);
        console.error('🔴 URL:', err.url);
        console.error('🔴 Message:', err.message);
        console.error('🔴 Error Object:', err.error);
        
        this.debugInfo += `  ❌ Error en ${elapsed}ms\n`;
        this.debugInfo += `  🔴 Status: ${err.status} (${err.statusText})\n`;
        this.debugInfo += `  🔴 Message: ${err.message}\n`;
        
        // ✅ REINTENTAR SI QUEDAN INTENTOS
        if (attemptsLeft > 1 && err.status === 0) {
          this.debugInfo += `  🔄 Reintentando en 2 segundos...\n`;
          
          console.log('🔄 Reintentando en 2 segundos...');
          
          setTimeout(() => {
            this.makeRequestWithRetry(code, email, attemptsLeft - 1);
          }, 2000);
          
          return;
        }
        
        // ✅ NO HAY MÁS INTENTOS O ERROR DIFERENTE
        this.loading = false;
        
        if (err.status === 0) {
          this.error = `❌ No se puede conectar con el backend después de ${4 - attemptsLeft} intentos\n\n` +
                      `⏱️ Tiempo total: ${elapsed}ms\n\n` +
                      `🔍 Posibles causas:\n` +
                      `1. El backend no está corriendo en puerto 8080\n` +
                      `2. CORS bloqueando la petición\n` +
                      `3. Firewall/Antivirus bloqueando\n` +
                      `4. El endpoint /spotify/getAuthorizationToken no existe\n\n` +
                      `💡 Verifica la consola del backend`;
        } else if (err.status === 404) {
          this.error = `❌ Endpoint no encontrado (404)\n\n${err.url}`;
        } else if (err.status === 500) {
          const errorMsg = err.error?.message || JSON.stringify(err.error);
          this.error = `❌ Error del servidor (500)\n\n${errorMsg}`;
        } else {
          this.error = `❌ Error ${err.status}\n\n${err.message}`;
        }
      }
    });
  }
  
  goToLogin() {
    sessionStorage.clear();
    this.router.navigate(['/login']);
  }
}