import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { interval, Subscription } from 'rxjs';
import { QueuePaymentComponent } from '../queue-payment/queue-payment';
import { UserService } from '../user';

// ==========================================
// INTERFACES
// ==========================================

interface SpotifyDevice {
  id: string;
  name: string;
  type: string;
  is_active: boolean;
  volume_percent: number;
}

interface SpotifyTrack {
  id: string;
  name: string;
  artists: { name: string }[];
  album: {
    name: string;
    images: { url: string }[];
  };
  duration_ms: number;
  uri: string;
}

interface PlayList {
  id: string;
  name: string;
  description: string;
  images: { url: string }[];
  tracks: {
    total: number;
  };
  uri: string;
}

interface TrackObject {
  track: SpotifyTrack;
  added_at: string;
}

interface SpotifyPlaybackState {
  device: SpotifyDevice;
  is_playing: boolean;
  item: SpotifyTrack;
  progress_ms: number;
}

// ==========================================
// COMPONENTE
// ==========================================

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, QueuePaymentComponent],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardComponent implements OnInit, OnDestroy {
  private http = inject(HttpClient);
  private router = inject(Router);  private userService = inject(UserService);
  private spotifyApiUrl = 'https://api.spotify.com/v1';
  private backendUrl = 'http://127.0.0.1:8080';

  // DATOS DEL USUARIO
  accessToken: string = '';
  userEmail: string = '';
  barName: string = '';
  latitude: string = '';
  longitude: string = '';
  signature: string = '';

  // DISPOSITIVOS
  devices: SpotifyDevice[] = [];
  currentDevice: SpotifyDevice | null = null;
  selectedDevice: SpotifyDevice | null = null;
  deviceError: string = '';
  activatingDevice: boolean = false;

  // PLAYLISTS
  playlists: PlayList[] = [];
  selectedPlaylist: PlayList | null = null;
  playlistError: string = '';

  // COLA DE REPRODUCCIÓN
  queue: TrackObject[] = [];
  currentPlaylistError: string = '';

  // REPRODUCCIÓN ACTUAL
  currentTrack: SpotifyTrack | null = null;
  isPlaying: boolean = false;
  progress: number = 0;
  duration: number = 0;
  volume: number = 50;

  // BÚSQUEDA
  searchQuery: string = '';
  searchResults: SpotifyTrack[] = [];
  searching: boolean = false;
  songError: string = '';
  titleFilter: string = '';
  artistFilter: string = '';

  // QUEUE PAYMENT SYSTEM
  showQueuePaymentModal: boolean = false;
  selectedTrackForQueue: SpotifyTrack | null = null;
  selectedPriceForQueue: number = 299; // Precio preseleccionado

  // PASSWORD MODAL
  showPasswordModal: boolean = false;
  passwordInput: string = '';
  pendingPlaylist: PlayList | null = null;

  // UI
  loading: boolean = true;
  error: string = '';
  showSearch: boolean = false;

  // Subscriptions
  private playbackSubscription?: Subscription;
  private queueSubscription?: Subscription;

  // ==========================================
  // INICIALIZACIÓN
  // ==========================================

  ngOnInit() {
    console.log('═══════════════════════════════════');
    console.log('🎵 DASHBOARD INICIALIZADO');
    console.log('═══════════════════════════════════');
    
    this.accessToken = sessionStorage.getItem('spotify_access_token') || '';
    this.userEmail = sessionStorage.getItem('userEmail') || '';
    this.barName = sessionStorage.getItem('barName') || '';
    this.latitude = sessionStorage.getItem('latitude') || '';
    this.longitude = sessionStorage.getItem('longitude') || '';
    this.signature = sessionStorage.getItem('signature') || '';
    
    console.log('📧 Email:', this.userEmail);
    console.log('🏪 Bar:', this.barName);
    console.log('📍 Coordenadas:', this.latitude, this.longitude);
    console.log('✍️ Firma:', this.signature ? '✅ Presente' : '❌ NO presente');
    console.log('🔑 Access Token:', this.accessToken ? '✅ Presente' : '❌ NO presente');
    
    if (!this.accessToken) {
      console.error('❌ No hay access token en sessionStorage');
      this.error = 'No se encontró el token de acceso. Redirigiendo al login...';
      this.loading = false;
      setTimeout(() => {
        sessionStorage.clear();
        this.router.navigate(['/login']);
      }, 3000);
      return;
    }
    
    if (!this.userEmail) {
      console.error('❌ No hay email en sessionStorage');
      this.error = 'Sesión inválida. Redirigiendo al login...';
      this.loading = false;
      setTimeout(() => {
        sessionStorage.clear();
        this.router.navigate(['/login']);
      }, 3000);
      return;
    }
    
    console.log('═══════════════════════════════════');
    console.log('✅ CREDENCIALES VÁLIDAS');
    console.log('═══════════════════════════════════');
    
    this.initializeDashboard();
  }

  ngOnDestroy() {
    console.log('👋 Dashboard destruido - Limpiando subscripciones');
    this.playbackSubscription?.unsubscribe();
    this.queueSubscription?.unsubscribe();
  }

  initializeDashboard() {
    console.log('🚀 Inicializando componentes del dashboard...');
    
    this.getDevices();
    this.getPlaylists();
    this.getCurrentPlayList();
    this.loadPlaybackState();
    this.startPlaybackPolling();
    this.startQueuePolling();

    this.loading = false;
    console.log('✅ Dashboard inicializado correctamente');
  }

  // ==========================================
  // GESTIÓN DE DISPOSITIVOS
  // ==========================================

  getDevices() {
    console.log('🔊 Cargando dispositivos...');
    this.resetErrors();
    
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${this.accessToken}`
    });

    this.http.get<{ devices: SpotifyDevice[] }>(
      `${this.spotifyApiUrl}/me/player/devices`,
      { headers }
    ).subscribe({
      next: (result) => {
        console.log('📱 Respuesta de Spotify:', result);
        
        this.devices = result.devices;
        this.currentDevice = this.devices.find(d => d.is_active) || null;
        this.selectedDevice = this.currentDevice || this.devices[0] || null;
        
        console.log('✅ Dispositivos cargados:', this.devices.length);
        
        if (this.devices.length > 0) {
          console.log('📱 Dispositivos disponibles:');
          this.devices.forEach((device, i) => {
            console.log(`  ${i + 1}. ${device.name} (${device.type}) - ${device.is_active ? '🟢 ACTIVO' : '⚪ Inactivo'}`);
          });
          
          if (!this.currentDevice && !this.activatingDevice) {
            console.log('⚠️ No hay dispositivos activos');
            console.log('🔄 Intentando activar el primer dispositivo...');
            this.activateFirstDevice();
          } else if (this.currentDevice) {
            console.log('🎵 Dispositivo activo:', this.currentDevice.name);
          }
        } else {
          this.deviceError = 'No hay dispositivos conectados';
          console.warn('⚠️ No hay dispositivos de Spotify disponibles');
          console.log('💡 Abre Spotify en tu computadora o móvil');
        }
      },
      error: (err: HttpErrorResponse) => {
        console.error('❌ Error al cargar dispositivos:', err);
        this.deviceError = 'Error al cargar dispositivos';
        
        if (err.status === 401) {
          console.error('❌ Token expirado o inválido');
          this.error = 'Token expirado. Redirigiendo al login...';
          setTimeout(() => {
            sessionStorage.clear();
            this.router.navigate(['/login']);
          }, 3000);
        }
      }
    });
  }

  activateFirstDevice() {
    if (this.devices.length === 0 || this.activatingDevice) {
      return;
    }

    this.activatingDevice = true;
    const firstDevice = this.devices[0];

    console.log('🔄 Activando dispositivo:', firstDevice.name);

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${this.accessToken}`,
      'Content-Type': 'application/json'
    });

    this.http.put(
      `${this.spotifyApiUrl}/me/player`,
      {
        device_ids: [firstDevice.id],
        play: false
      },
      { headers }
    ).subscribe({
      next: () => {
        console.log('✅ Dispositivo activado:', firstDevice.name);
        this.activatingDevice = false;
        
        setTimeout(() => {
          this.getDevices();
        }, 1000);
      },
      error: (err) => {
        console.error('❌ Error al activar dispositivo:', err);
        this.activatingDevice = false;
        
        this.deviceError = 
          `No se pudo activar "${firstDevice.name}".\n\n` +
          `Por favor:\n` +
          `1. Abre Spotify Desktop\n` +
          `2. Reproduce cualquier canción\n` +
          `3. Haz clic en "Recargar"`;
      }
    });
  }

  // ==========================================
  // GESTIÓN DE PLAYLISTS
  // ==========================================

  getPlaylists() {
    console.log('📂 Cargando playlists...');
    this.resetErrors();
    
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${this.accessToken}`
    });

    this.http.get<{ items: PlayList[] }>(
      `${this.spotifyApiUrl}/me/playlists?limit=50`,
      { headers }
    ).subscribe({
      next: (response) => {
        this.playlists = response.items;
        console.log('✅ Playlists cargadas:', this.playlists.length);
        
        if (this.playlists.length === 0) {
          this.playlistError = 'No tienes playlists disponibles';
          console.warn('⚠️ No hay playlists');
        } else {
          console.log('📂 Playlists disponibles:');
          this.playlists.slice(0, 5).forEach((pl, i) => {
            console.log(`  ${i + 1}. ${pl.name} (${pl.tracks.total} canciones)`);
          });
          if (this.playlists.length > 5) {
            console.log(`  ... y ${this.playlists.length - 5} más`);
          }
        }
      },
      error: (err) => {
        console.error('❌ Error al cargar playlists:', err);
        this.playlistError = 'Error al cargar playlists';
      }
    });
  }

  getCurrentPlayList() {
    console.log('🎵 Cargando cola de reproducción de Spotify...');
    this.resetErrors();
    
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${this.accessToken}`
    });
    
    // Obtener la cola actual de Spotify (no la playlist)
    this.http.get<{ queue: SpotifyTrack[] }>(
      `${this.spotifyApiUrl}/me/player/queue`,
      { headers }
    ).subscribe({
      next: (response) => {
        // Convertir las canciones de la cola al formato TrackObject
        if (response.queue && response.queue.length > 0) {
          this.queue = response.queue.map(track => ({
            track: track,
            added_at: new Date().toISOString()
          }));
          console.log('✅ Cola de Spotify cargada:', this.queue.length, 'canciones');
          console.log('🎵 Primeras canciones en cola:');
          this.queue.slice(0, 3).forEach((item, i) => {
            console.log(`  ${i + 1}. ${item.track.name} - ${item.track.artists[0].name}`);
          });
        } else {
          this.queue = [];
          console.log('ℹ️ La cola está vacía');
        }
      },
      error: (err) => {
        // Si no hay cola activa o error 404, simplemente vaciar
        if (err.status === 404) {
          console.log('ℹ️ No hay cola activa en Spotify');
          this.queue = [];
        } else {
          console.error('❌ Error al cargar cola:', err);
          this.currentPlaylistError = 'Error al cargar la cola actual';
        }
      }
    });
  }

  selectPlaylist(playlist: PlayList) {
    console.log('📂 Playlist seleccionada:', playlist.name);
    this.selectedPlaylist = playlist;
    this.getCurrentPlayList();
    
    // Auto-reproducir si hay dispositivo seleccionado
    if (this.selectedDevice && playlist.uri) {
      this.playPlaylist(playlist);
    }
  }

  requestPasswordAndSelectPlaylist(playlist: PlayList) {
    // Guardar la playlist pendiente y mostrar el modal
    this.pendingPlaylist = playlist;
    this.passwordInput = '';
    this.showPasswordModal = true;
  }

  cancelPasswordPrompt() {
    this.showPasswordModal = false;
    this.passwordInput = '';
    this.pendingPlaylist = null;
  }

  confirmPasswordPrompt() {
    if (!this.passwordInput) {
      alert('⚠️ Por favor introduce una contraseña');
      return;
    }

    if (!this.pendingPlaylist) {
      this.cancelPasswordPrompt();
      return;
    }

    const email = sessionStorage.getItem('userEmail');
    if (!email) {
      alert('⚠️ No se encontró el email del usuario');
      this.cancelPasswordPrompt();
      return;
    }

    console.log('🔐 Verificando contraseña...');

    // Verificar la contraseña con el backend
    this.http.post(`${this.backendUrl}/user/verifyPassword`, 
      { email, password: this.passwordInput },
      { withCredentials: true, responseType: 'text' }
    ).subscribe({
      next: (response) => {
        console.log('✅ Contraseña correcta');
        const playlistToSelect = this.pendingPlaylist;
        this.cancelPasswordPrompt();
        if (playlistToSelect) {
          this.selectPlaylist(playlistToSelect);
        }
      },
      error: (err) => {
        console.error('❌ Contraseña incorrecta:', err);
        alert('❌ Contraseña incorrecta. Solo el dueño del bar puede cambiar la playlist.');
        this.passwordInput = '';
      }
    });
  }

  playPlaylist(playlist: PlayList) {
    if (!this.selectedDevice) {
      alert('⚠️ Selecciona un dispositivo primero');
      return;
    }

    console.log('▶️ Reproduciendo playlist:', playlist.name);
    console.log('🔊 En dispositivo:', this.selectedDevice.name);

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${this.accessToken}`,
      'Content-Type': 'application/json'
    });

    const body = {
      context_uri: `spotify:playlist:${playlist.id}`,
      device_id: this.selectedDevice.id
    };

    this.http.put(
      `${this.spotifyApiUrl}/me/player/play`,
      body,
      { headers }
    ).subscribe({
      next: () => {
        console.log('✅ Reproducción iniciada');
        this.isPlaying = true;
        setTimeout(() => this.loadPlaybackState(), 1000);
      },
      error: (err) => {
        console.error('❌ Error al reproducir playlist:', err);
        alert('❌ Error al reproducir.\n\nAsegúrate de:\n- Tener Spotify abierto\n- Que el dispositivo esté conectado\n- Tener Spotify Premium');
      }
    });
  }

  // ==========================================
  // ESTADO DE REPRODUCCIÓN
  // ==========================================

  loadPlaybackState() {
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${this.accessToken}`
    });

    this.http.get<SpotifyPlaybackState>(
      `${this.spotifyApiUrl}/me/player`,
      { headers }
    ).subscribe({
      next: (state) => {
        if (state && state.item) {
          this.currentTrack = state.item;
          this.isPlaying = state.is_playing;
          this.progress = state.progress_ms;
          this.duration = state.item.duration_ms;
          
          if (state.device) {
            this.volume = state.device.volume_percent;
            this.currentDevice = state.device;
          }
          
          console.log('🎵 Reproduciendo:', state.item.name, '-', state.item.artists[0].name);
        }
      },
      error: (err) => {
        if (err.status !== 204) {
          console.log('ℹ️ No hay reproducción activa');
        }
      }
    });
  }

  startPlaybackPolling() {
    console.log('⏰ Iniciando polling cada 5 segundos');
    this.playbackSubscription = interval(5000).subscribe(() => {
      this.loadPlaybackState();
    });
  }

  startQueuePolling() {
    console.log('⏰ Iniciando polling de cola cada 1 segundo');
    this.queueSubscription = interval(1000).subscribe(() => {
      // Siempre actualizar la cola real de Spotify, no depende de selectedPlaylist
      this.getCurrentPlayList();
    });
  }

  // ==========================================
  // BÚSQUEDA DE CANCIONES
  // ==========================================

  search() {
    console.log('🔍 Iniciando búsqueda...');
    console.log('  Título:', this.titleFilter);
    console.log('  Artista:', this.artistFilter);
    
    let query = '';
    
    if (this.titleFilter.trim()) {
      query += `track:${this.titleFilter.trim()} `;
    }
    
    if (this.artistFilter.trim()) {
      query += `artist:${this.artistFilter.trim()}`;
    }
    
    if (!query.trim() && this.searchQuery.trim()) {
      query = this.searchQuery.trim();
    }
    
    if (!query.trim()) {
      console.warn('⚠️ No hay parámetros de búsqueda');
      return;
    }

    console.log('🔍 Query final:', query);
    this.searching = true;
    this.songError = '';

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${this.accessToken}`
    });

    this.http.get<any>(
      `${this.spotifyApiUrl}/search?q=${encodeURIComponent(query)}&type=track&limit=20`,
      { headers }
    ).subscribe({
      next: (response) => {
        this.searchResults = response.tracks.items;
        this.searching = false;
        
        console.log('✅ Búsqueda completada:', this.searchResults.length, 'resultados');
        console.log('🎵 Primeros resultados:');
        this.searchResults.slice(0, 5).forEach((track, i) => {
          console.log(`  ${i + 1}. ${track.name} - ${track.artists[0].name}`);
        });
        
        if (this.searchResults.length === 0) {
          this.songError = 'No se encontraron canciones con esos criterios';
          console.warn('⚠️ Sin resultados');
        }
      },
      error: (err) => {
        this.searching = false;
        this.songError = 'Error al buscar canciones';
        console.error('❌ Error en búsqueda:', err);
      }
    });
  }

  openSearch() {
    this.showSearch = true;
    setTimeout(() => {
      const titleInput = document.querySelector('.search-input') as HTMLInputElement;
      if (titleInput) titleInput.focus();
    }, 100);
  }

  closeSearch() {
    this.showSearch = false;
    this.clearSearch();
  }

  clearSearch() {
    this.titleFilter = '';
    this.artistFilter = '';
    this.searchResults = [];
    this.songError = '';
  }

  addToQueue(track: SpotifyTrack) {
    if (!this.selectedDevice) {
      alert('⚠️ Selecciona un dispositivo primero');
      return;
    }

    console.log('➕ Añadiendo a la cola:', track.name);

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${this.accessToken}`
    });

    const deviceId = this.selectedDevice.id;

    this.http.post(
      `${this.spotifyApiUrl}/me/player/queue?uri=${encodeURIComponent(track.uri)}&device_id=${deviceId}`,
      null,
      { 
        headers, 
        observe: 'response',
        responseType: 'text'
      }
    ).subscribe({
      next: (response: any) => {
        console.log('✅ Canción añadida a la cola - Status:', response.status);
        
        this.queue.push({
          track: track,
          added_at: new Date().toISOString()
        });
        
        // Actualizar cola inmediatamente
        if (this.selectedPlaylist) {
          setTimeout(() => this.getCurrentPlayList(), 500);
        }
        
        // Cerrar modal de búsqueda si está abierto
        if (this.showSearch) {
          this.showSearch = false;
        }
      },
      error: (err) => {
        // 204 No Content es ÉXITO en Spotify API
        if (err.status === 204 || err.status === 0) {
          console.log('✅ Canción añadida a la cola (204 No Content)');
          
          this.queue.push({
            track: track,
            added_at: new Date().toISOString()
          });
          
          // Actualizar cola inmediatamente
          setTimeout(() => this.getCurrentPlayList(), 500);
          
          // Cerrar modal de búsqueda si está abierto
          if (this.showSearch) {
            this.showSearch = false;
          }
        } else {
          console.error('❌ Error real al añadir a la cola:', err);
          alert(`❌ Error al añadir canción: ${err.message || 'Error desconocido'}`);
        }
      }
    });
  }

  playTrack(track: SpotifyTrack) {
    const deviceId = this.selectedDevice?.id;
    
    if (!deviceId) {
      alert('⚠️ Selecciona un dispositivo primero');
      return;
    }

    console.log('▶️ Reproduciendo:', track.name);

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${this.accessToken}`,
      'Content-Type': 'application/json'
    });

    const body = { uris: [track.uri] };

    this.http.put(
      `${this.spotifyApiUrl}/me/player/play?device_id=${deviceId}`,
      body,
      { headers }
    ).subscribe({
      next: () => {
        console.log('✅ Reproducción iniciada');
        this.currentTrack = track;
        this.isPlaying = true;
        this.showSearch = false;
        this.searchQuery = '';
        this.searchResults = [];
      },
      error: (err) => {
        console.error('❌ Error al reproducir:', err);
        alert('❌ Error al reproducir.\n\nAsegúrate de tener Spotify abierto en el dispositivo.');
      }
    });
  }

  // ==========================================
  // QUEUE PAYMENT SYSTEM
  // ==========================================

  openQueuePaymentModal(track: SpotifyTrack, price: number = 299) {
    if (!this.selectedDevice) {
      alert('⚠️ Selecciona un dispositivo primero');
      return;
    }

    console.log('💳 Abriendo modal de pago para:', track.name);
    console.log('💰 Precio preseleccionado:', price === 199 ? '1.99€ (Adelantar)' : '2.99€ (Nueva)');
    this.selectedTrackForQueue = track;
    this.selectedPriceForQueue = price;
    this.showQueuePaymentModal = true;
  }

  closeQueuePaymentModal() {
    console.log('🔒 Cerrando modal de pago');
    this.showQueuePaymentModal = false;
    this.selectedTrackForQueue = null;
    
    setTimeout(() => {
      this.loadPlaybackState();
    }, 1000);
  }

  // ==========================================
  // CONTROLES DE REPRODUCCIÓN
  // ==========================================

  togglePlay() {
    console.log('⏯️ Toggle play/pause');
    
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${this.accessToken}`
    });

    const url = this.isPlaying 
      ? `${this.spotifyApiUrl}/me/player/pause`
      : `${this.spotifyApiUrl}/me/player/play`;

    this.http.put(url, {}, { headers }).subscribe({
      next: () => {
        this.isPlaying = !this.isPlaying;
        console.log(this.isPlaying ? '▶️ Reanudado' : '⏸️ Pausado');
      },
      error: (err) => console.error('❌ Error:', err)
    });
  }

  nextTrack() {
    console.log('⏭️ Siguiente canción');
    
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${this.accessToken}`
    });

    this.http.post(
      `${this.spotifyApiUrl}/me/player/next`,
      {},
      { headers }
    ).subscribe({
      next: () => {
        console.log('✅ Siguiente canción');
        setTimeout(() => this.loadPlaybackState(), 500);
      },
      error: (err) => console.error('❌ Error:', err)
    });
  }

  previousTrack() {
    console.log('⏮️ Canción anterior');
    
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${this.accessToken}`
    });

    this.http.post(
      `${this.spotifyApiUrl}/me/player/previous`,
      {},
      { headers }
    ).subscribe({
      next: () => {
        console.log('✅ Canción anterior');
        setTimeout(() => this.loadPlaybackState(), 500);
      },
      error: (err) => console.error('❌ Error:', err)
    });
  }

  changeVolume(event: Event) {
    const input = event.target as HTMLInputElement;
    this.volume = parseInt(input.value);
    
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${this.accessToken}`
    });

    this.http.put(
      `${this.spotifyApiUrl}/me/player/volume?volume_percent=${this.volume}`,
      {},
      { headers }
    ).subscribe({
      next: () => {
        console.log('🔊 Volumen:', this.volume);
      },
      error: (err) => console.error('❌ Error:', err)
    });
  }

  // ==========================================
  // UTILIDADES
  // ==========================================

  resetErrors() {
    this.deviceError = '';
    this.playlistError = '';
    this.currentPlaylistError = '';
    this.songError = '';
  }

  formatTime(ms: number): string {
    const minutes = Math.floor(ms / 60000);
    const seconds = Math.floor((ms % 60000) / 1000);
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }

  getProgressPercentage(): number {
    if (this.duration === 0) return 0;
    return (this.progress / this.duration) * 100;
  }

  logout() {
    if (confirm('¿Seguro que quieres cerrar sesión?')) {
      console.log('👋 Cerrando sesión...');
      sessionStorage.clear();
      this.router.navigate(['/login']);
    }
  }

  deleteAccount() {
    const confirmation = confirm(
      '⚠️ ¿ESTÁS SEGURO DE ELIMINAR TU CUENTA?\n\n' +
      'Esta acción es PERMANENTE y NO se puede deshacer.\n\n' +
      'Se eliminarán:\n' +
      '- Tu cuenta de usuario\n' +
      '- Todas tus configuraciones\n' +
      '- Historial de pagos\n\n' +
      'Haz clic en OK para confirmar la eliminación.'
    );

    if (!confirmation) {
      return;
    }

    // Segunda confirmación
    const finalConfirmation = confirm(
      '⚠️ Última confirmación\n\n' +
      '¿Realmente deseas eliminar tu cuenta "' + this.barName + '"?\n\n' +
      'Esta acción es IRREVERSIBLE.'
    );

    if (!finalConfirmation) {
      return;
    }

    console.log('🗑️ Eliminando cuenta:', this.userEmail);

    this.userService.deleteAccount(this.userEmail).subscribe({
      next: () => {
        alert('✅ Cuenta eliminada exitosamente.\n\nLamentamos verte partir.');
        console.log('✅ Cuenta eliminada correctamente');
        sessionStorage.clear();
        this.router.navigate(['/login']);
      },
      error: (err) => {
        console.error('❌ Error al eliminar cuenta:', err);
        alert('❌ Error al eliminar la cuenta: ' + (err.error?.message || err.message || 'Error desconocido'));
      }
    });
  }
}