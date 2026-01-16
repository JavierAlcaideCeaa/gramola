import { Component, inject, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UserService } from '../user';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register implements AfterViewInit {
  private userService = inject(UserService);
  private router = inject(Router);
  
  @ViewChild('signatureCanvas') signatureCanvas!: ElementRef<HTMLCanvasElement>;
  private ctx!: CanvasRenderingContext2D;
  private isDrawing: boolean = false;
  signatureDrawn: boolean = false;
  signatureData: string = '';
  
  barName: string = '';
  email: string = '';
  pwd1: string = '';
  pwd2: string = '';
  clientId: string = '';
  clientSecret: string = '';
  address: string = ''; // NUEVO: DirecciÃ³n postal del bar
  latitude: number | null = null;
  longitude: number | null = null;
  locationDetected: boolean = false;
  isLoading: boolean = false;
  
  ngAfterViewInit() {
    const canvas = this.signatureCanvas.nativeElement;
    canvas.width = canvas.offsetWidth;
    canvas.height = 200;
    this.ctx = canvas.getContext('2d')!;
    this.ctx.strokeStyle = '#000';
    this.ctx.lineWidth = 2;
    this.ctx.lineCap = 'round';
  }

  register() {
    // Validaciones del lado del cliente
    if (!this.barName || !this.email || !this.pwd1 || !this.pwd2 || 
        !this.clientId || !this.clientSecret) {
      alert('âš ï¸ Por favor complete todos los campos obligatorios');
      return;
    }
    
    if (!this.locationDetected || this.latitude === null || this.longitude === null) {
      alert('âš ï¸ Por favor detecta tu ubicaciÃ³n GPS antes de registrarte.\n\nLa aplicaciÃ³n solo funciona dentro del bar (radio de 100 metros).');
      return;
    }
    
    if (!this.signatureDrawn || !this.signatureData) {
      alert('âš ï¸ Por favor firma en el recuadro antes de registrarte.');
      return;
    }
    
    if (this.pwd1 !== this.pwd2) {
      alert('âš ï¸ Las contraseÃ±as no coinciden');
      return;
    }
    
    if (!this.isValidEmail(this.email)) {
      alert('âš ï¸ Por favor ingrese un correo electrÃ³nico vÃ¡lido');
      return;
    }
    
    if (this.pwd1.length < 6) {
      alert('âš ï¸ La contraseÃ±a debe tener al menos 6 caracteres');
      return;
    }
    
    if (this.barName.trim().length < 3) {
      alert('âš ï¸ El nombre del bar debe tener al menos 3 caracteres');
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
      this.clientSecret,
      this.address || `GPS: ${this.latitude}, ${this.longitude}`, // DirecciÃ³n como referencia GPS
      this.latitude!, // Coordenadas GPS detectadas
      this.longitude!,
      this.signatureData // Firma digital
    ).subscribe({
      next: () => {
        // âœ… Solo recibimos 200 OK (void)
        // NO se muestra el token aquÃ­
        this.isLoading = false;
        
        console.log('âœ… Registro exitoso para:', this.email);
        
        // Mostrar mensaje GENÃ‰RICO sin mencionar el token
        alert(
          'âœ… Â¡Registro Exitoso!\n\n' +
          'Hemos enviado un correo de confirmaciÃ³n a:\n' +
          this.email + '\n\n' +
          'ðŸ“§ Por favor, revisa tu bandeja de entrada (y spam)\n' +
          'y haz clic en el enlace de confirmaciÃ³n.\n\n' +
          'â±ï¸ El enlace expirarÃ¡ en 30 minutos.\n\n' +
          '(En desarrollo, el correo aparece en la consola del servidor)'
        );
        
        // Limpiar formulario
        this.clearForm();
        
        // Redirigir al login despuÃ©s de 5 segundos
        setTimeout(() => {
          this.router.navigate(['/login'], {
            queryParams: { 
              message: 'Por favor confirma tu cuenta antes de iniciar sesiÃ³n' 
            }
          });
        }, 5000);
      },
      error: (err) => {
        this.isLoading = false;
        
        // Mensajes de error personalizados segÃºn cÃ³digo HTTP
        let errorMessage = 'Error desconocido';
        let shouldRedirectToLogin = false;
        
        if (err.status === 409) {
          // Usuario ya existe y estÃ¡ activo
          errorMessage = 
            'âš ï¸ Este bar ya estÃ¡ registrado y activo.\n\n' +
            'Â¿Deseas iniciar sesiÃ³n?';
          shouldRedirectToLogin = true;
          
        } else if (err.status === 406) {
          // Datos invÃ¡lidos
          errorMessage = err.error?.message || 
            'Datos invÃ¡lidos.\n\n' +
            'Por favor verifica:\n' +
            'â€¢ Las contraseÃ±as coinciden\n' +
            'â€¢ El email es vÃ¡lido\n' +
            'â€¢ Todos los campos estÃ¡n completos';
          
        } else if (err.status === 0) {
          // Error de red
          errorMessage = 
            'âŒ Error de conexiÃ³n.\n\n' +
            'No se pudo conectar con el servidor.\n' +
            'Verifica que el backend estÃ© corriendo en:\n' +
            'http://localhost:8080';
          
        } else {
          errorMessage = err.error?.message || err.message || 
            'Error al registrar el bar.\n\nIntÃ©ntalo nuevamente.';
        }
        
        alert('âŒ Error en el registro:\n\n' + errorMessage);
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
    this.address = '';
    this.latitude = null;
    this.longitude = null;
    this.locationDetected = false;
    this.clearSignature();
  }
  
  /**
   * Detecta la ubicaciÃ³n GPS actual del usuario para el registro del bar
   */
  async detectLocation() {
    if (!navigator.geolocation) {
      alert('âŒ Tu navegador no soporta geolocalizaciÃ³n');
      return;
    }
    
    this.isLoading = true;
    
    try {
      const position = await this.getUserLocation();
      this.latitude = position.coords.latitude;
      this.longitude = position.coords.longitude;
      this.locationDetected = true;
      this.address = `GPS: ${this.latitude.toFixed(6)}, ${this.longitude.toFixed(6)}`;
      
      console.log('ðŸ“ UbicaciÃ³n detectada:', this.latitude, this.longitude);
      alert(`âœ… UbicaciÃ³n detectada correctamente\n\nðŸ“ Coordenadas:\nLatitud: ${this.latitude.toFixed(6)}\nLongitud: ${this.longitude.toFixed(6)}`);
      
    } catch (error: any) {
      console.error('âŒ Error detectando ubicaciÃ³n:', error);
      
      let errorMessage = 'Error desconocido';
      if (error.message) {
        errorMessage = error.message;
      }
      
      alert('âŒ No se pudo detectar tu ubicaciÃ³n:\n\n' + errorMessage + 
            '\n\nAsegÃºrate de permitir el acceso a la ubicaciÃ³n en tu navegador.');
    } finally {
      this.isLoading = false;
    }
  }
  
  /**
   * Obtiene la ubicaciÃ³n actual del usuario usando Geolocation API
   */
  private getUserLocation(): Promise<GeolocationPosition> {
    return new Promise((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(
        (position) => resolve(position),
        (error) => {
          console.error('Error obteniendo ubicaciÃ³n:', error);
          let errorMessage = 'Error obteniendo ubicaciÃ³n';
          
          switch(error.code) {
            case error.PERMISSION_DENIED:
              errorMessage = 'Permiso de ubicaciÃ³n denegado';
              break;
            case error.POSITION_UNAVAILABLE:
              errorMessage = 'UbicaciÃ³n no disponible';
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
  
  /**
   * Inicia el dibujo en el canvas
   */
  startDrawing(event: MouseEvent | TouchEvent) {
    this.isDrawing = true;
    const coords = this.getCoordinates(event);
    this.ctx.beginPath();
    this.ctx.moveTo(coords.x, coords.y);
  }
  
  /**
   * Dibuja en el canvas mientras se mueve el mouse/dedo
   */
  draw(event: MouseEvent | TouchEvent) {
    if (!this.isDrawing) return;
    
    event.preventDefault();
    const coords = this.getCoordinates(event);
    this.ctx.lineTo(coords.x, coords.y);
    this.ctx.stroke();
    this.signatureDrawn = true;
  }
  
  /**
   * Detiene el dibujo
   */
  stopDrawing() {
    if (this.isDrawing) {
      this.isDrawing = false;
      this.captureSignature();
    }
  }
  
  /**
   * Obtiene las coordenadas del evento (mouse o touch)
   */
  private getCoordinates(event: MouseEvent | TouchEvent): { x: number, y: number } {
    const canvas = this.signatureCanvas.nativeElement;
    const rect = canvas.getBoundingClientRect();
    
    if (event instanceof MouseEvent) {
      return {
        x: event.clientX - rect.left,
        y: event.clientY - rect.top
      };
    } else {
      const touch = event.touches[0];
      return {
        x: touch.clientX - rect.left,
        y: touch.clientY - rect.top
      };
    }
  }
  
  /**
   * Captura la firma como base64
   */
  private captureSignature() {
    const canvas = this.signatureCanvas.nativeElement;
    this.signatureData = canvas.toDataURL('image/png');
    console.log('âœï¸ Firma capturada');
  }
  
  /**
   * Limpia el canvas de firma
   */
  clearSignature() {
    const canvas = this.signatureCanvas.nativeElement;
    this.ctx.clearRect(0, 0, canvas.width, canvas.height);
    this.signatureDrawn = false;
    this.signatureData = '';
    console.log('ðŸ—‘ï¸ Firma limpiada');
  }

}
