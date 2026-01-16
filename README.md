# Gramola - Sistema de Jukebox Digital

## Descripción del Proyecto

Gramola es una aplicación web completa que permite a los propietarios de bares gestionar un sistema de jukebox digital conectado con Spotify. Los clientes pueden buscar canciones, pagarlas a través de Stripe y añadirlas a la cola de reproducción. El sistema incluye características avanzadas como geofencing, firma digital y recuperación de contraseña.

## Tecnologías Utilizadas

### Backend
- **Java 21** con Spring Boot 3.4.1
- **Spring Data JPA** para persistencia
- **MySQL** como base de datos
- **Stripe API** para procesamiento de pagos
- **Spotify Web API** para integración musical
- **JavaMail** para envío de correos
- **Nominatim API** para geocoding

### Frontend
- **Angular 19**  
- **TypeScript 5.6**
- **Stripe.js** para formularios de pago
- **HTML5 Canvas** para firma digital

### Testing
- **JUnit 5** para tests unitarios
- **Selenium WebDriver** para tests end-to-end
- **ChromeDriver** para automatización del navegador

## Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENTE (Navegador)                       │
│                    Angular Frontend                          │
│                  http://127.0.0.1:4200                       │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      │ HTTP/REST
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                 BACKEND (Spring Boot)                        │
│                http://localhost:8080                         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Controllers                                          │   │
│  │  - UserController      (Registro, Login, Password)  │   │
│  │  - PaymentController   (Suscripciones)              │   │
│  │  - SpotifyController   (OAuth, Tokens)              │   │
│  │  - QueuePaymentController (Pagos de cola)           │   │
│  └────────────┬─────────────────────────────────────────┘   │
│               │                                              │
│  ┌────────────▼─────────────────────────────────────────┐   │
│  │ Services                                             │   │
│  │  - UserService          (Lógica de usuarios)        │   │
│  │  - PaymentService       (Stripe suscripciones)      │   │
│  │  - QueuePaymentService  (Stripe pagos cola)         │   │
│  │  - SpotifyService       (API Spotify)               │   │
│  │  - EmailService         (Envío de correos)          │   │
│  │  - GeocodingService     (Conversión direcciones)    │   │
│  │  - LocationService      (Cálculo distancias)        │   │
│  └────────────┬─────────────────────────────────────────┘   │
│               │                                              │
│  ┌────────────▼─────────────────────────────────────────┐   │
│  │ DAOs (Repositories)                                  │   │
│  │  - UserDao                                           │   │
│  │  - PriceDao                                          │   │
│  │  - SpotifyTokenDao                                   │   │
│  │  - QueuePaymentTransactionDao                        │   │
│  └────────────┬─────────────────────────────────────────┘   │
└───────────────┼──────────────────────────────────────────────┘
                │
┌───────────────▼──────────────────────────────────────────────┐
│                     MySQL Database                            │
│  Tables:                                                      │
│    - users                (Propietarios de bares)            │
│    - prices               (Tarifas configurables)             │
│    - spotify_tokens       (Tokens de autenticación)           │
│    - queue_transactions   (Historial de pagos)                │
└───────────────────────────────────────────────────────────────┘

                ┌─────────────┐
                │             │
┌───────────────▼──────┐  ┌──▼─────────────────┐
│   Spotify API        │  │   Stripe API       │
│   - OAuth 2.0        │  │   - PaymentIntents │
│   - Player Control   │  │   - Subscriptions  │
│   - Queue Management │  │   - Test Cards     │
└──────────────────────┘  └────────────────────┘
```

## Funcionalidades Principales

### 1. Registro y Autenticación

**Flujo de Registro:**
1. El propietario del bar se registra con:
   - Email
   - Contraseña
   - Nombre del bar
   - Credenciales de Spotify (Client ID y Secret)
   - Dirección física (se convierte automáticamente a coordenadas GPS)
   - Firma digital (canvas HTML5)

2. El sistema:
   - Valida todos los campos
   - Encripta la contraseña con SHA-256
   - Convierte la dirección a coordenadas GPS usando Nominatim
   - Genera un token de confirmación único
   - Envía email de confirmación con enlace

3. Al confirmar el email:
   - Se activa la cuenta
   - Se redirige a pago de suscripción (Stripe)

4. Tras completar el pago:
   - Se inicia OAuth 2.0 con Spotify
   - Se obtienen tokens de acceso
   - Se redirige al dashboard

**Código relevante:**
- [UserService.java](gramolaJavier/src/main/java/edu/uclm/esi/gramolaJavier/services/UserService.java) - Lógica de registro
- [EmailService.java](gramolaJavier/src/main/java/edu/uclm/esi/gramolaJavier/services/EmailService.java) - Envío de correos
- [GeocodingService.java](gramolaJavier/src/main/java/edu/uclm/esi/gramolaJavier/services/GeocodingService.java) - Conversión GPS

### 2. Dashboard del Propietario

El dashboard permite:

- **Gestión de dispositivos Spotify:**
  - Ver dispositivos conectados (móvil, PC, speakers)
  - Activar/desactivar dispositivos
  - Seleccionar dispositivo de reproducción

- **Control de reproducción:**
  - Reproducir/pausar
  - Siguiente/anterior canción
  - Ver canción actual
  - Ver cola de reproducción en tiempo real

- **Gestión de playlists:**
  - Listar playlists del usuario
  - Cambiar playlist activa (requiere verificación de contraseña)
  - Reproducir desde playlist

- **Búsqueda y encolamiento:**
  - Buscar canciones en Spotify
  - Ver resultados con caratula, artista y álbum
  - Añadir canciones directamente a la cola

**Actualización en tiempo real:**
- Polling cada 5 segundos para estado de reproducción
- Polling cada 1 segundo para cola de reproducción
- Muestra información actualizada sin recargar la página

**Código relevante:**
- [dashboard.ts](gramolaJavier/gramolaJa/src/app/dashboard/dashboard.ts) - Lógica del dashboard
- [dashboard.html](gramolaJavier/gramolaJa/src/app/dashboard/dashboard.html) - Interfaz de usuario

### 3. Sistema de Pagos

#### a) Suscripción Inicial (Una vez)
- **Precio:** 4.99€/mes (configurable en BD)
- **Método:** Stripe Payment Intents
- **Flujo:**
  1. Tras confirmar email, se crea PaymentIntent
  2. Cliente ingresa datos de tarjeta en iframe de Stripe
  3. Backend confirma el pago
  4. Se marca la cuenta como "pagada"
  5. Se inicia OAuth con Spotify

#### b) Pagos de Cola (Por canción)
- **Precios:**
  - 2.99€ - Añadir canción nueva a la cola
  - 1.99€ - Adelantar canción existente en la cola
- **Método:** Stripe Payment Intents con metadata
- **Flujo:**
  1. Cliente hace clic en "Añadir"
  2. Se abre modal con formulario
  3. Cliente ingresa:
     - Nombre
     - Ubicación actual (GPS)
  4. Sistema verifica geofencing (< 100 metros del bar)
  5. Si está cerca:
     - Se crea PaymentIntent en Stripe
     - Cliente ingresa tarjeta
     - Se confirma pago
     - Se añade canción a cola de Spotify
     - Se guarda transacción en BD

**Código relevante:**
- [PaymentService.java](gramolaJavier/src/main/java/edu/uclm/esi/gramolaJavier/services/PaymentService.java) - Suscripciones
- [QueuePaymentService.java](gramolaJavier/src/main/java/edu/uclm/esi/gramolaJavier/services/QueuePaymentService.java) - Pagos de cola
- [queue-payment.ts](gramolaJavier/gramolaJa/src/app/queue-payment/queue-payment.ts) - Modal de pago

### 4. Integración con Spotify

**OAuth 2.0 Authorization Code Flow:**

1. **Autorización:**
   ```
   Frontend → Spotify Authorization Endpoint
   https://accounts.spotify.com/authorize?
     client_id=XXX&
     response_type=code&
     redirect_uri=http://127.0.0.1:4200/callback&
     scope=user-read-playback-state user-modify-playback-state...
   ```

2. **Callback:**
   ```
   Spotify → Frontend (callback)
   http://127.0.0.1:4200/callback?code=XXX&state=YYY
   ```

3. **Intercambio de tokens:**
   ```
   Frontend → Backend → Spotify Token Endpoint
   POST https://accounts.spotify.com/api/token
   Authorization: Basic base64(client_id:client_secret)
   Body: grant_type=authorization_code&code=XXX&redirect_uri=...
   ```

4. **Respuesta:**
   ```json
   {
     "access_token": "BQA...",
     "refresh_token": "AQD...",
     "expires_in": 3600,
     "token_type": "Bearer"
   }
   ```

5. **Almacenamiento:**
   - Access Token y Refresh Token se guardan en BD
   - Se calculan fechas de expiración
   - El sistema refresca tokens automáticamente cuando expiran

**APIs utilizadas:**
- `GET /v1/me/player/devices` - Listar dispositivos
- `PUT /v1/me/player` - Activar dispositivo
- `GET /v1/me/player` - Estado actual
- `PUT /v1/me/player/play` - Reproducir
- `POST /v1/me/player/next` - Siguiente canción
- `POST /v1/me/player/queue` - Añadir a cola
- `GET /v1/me/playlists` - Listar playlists
- `GET /v1/search` - Buscar canciones

**Código relevante:**
- [SpotifyService.java](gramolaJavier/src/main/java/edu/uclm/esi/gramolaJavier/services/SpotifyService.java) - Lógica de Spotify
- [callback.ts](gramolaJavier/gramolaJa/src/app/callback/callback.ts) - Manejo de callback OAuth

### 5. Geofencing y Ubicación

**Sistema de Verificación de Proximidad:**

1. **Registro:**
   - Al registrarse, se convierte la dirección del bar a coordenadas GPS
   - Se usa la API de Nominatim (OpenStreetMap)
   - Las coordenadas se guardan en la BD

2. **Pago de Cola:**
   - El navegador solicita ubicación del cliente (Geolocation API)
   - Se envían coordenadas al backend
   - El backend calcula distancia usando fórmula de Haversine:
     ```
     a = sin²(Δφ/2) + cos φ1 ⋅ cos φ2 ⋅ sin²(Δλ/2)
     c = 2 ⋅ atan2(√a, √(1−a))
     d = R ⋅ c
     ```
     donde R = 6371000 metros (radio de la Tierra)

3. **Validación:**
   - Si distancia < 100 metros → Permitir pago
   - Si distancia >= 100 metros → Rechazar con mensaje de error

**Código relevante:**
- [LocationService.java](gramolaJavier/src/main/java/edu/uclm/esi/gramolaJavier/services/LocationService.java) - Cálculo de distancias
- [GeocodingService.java](gramolaJavier/src/main/java/edu/uclm/esi/gramolaJavier/services/GeocodingService.java) - Geocoding

### 6. Recuperación de Contraseña

**Flujo completo:**

1. Usuario hace clic en "¿Olvidaste tu contraseña?"
2. Ingresa su email
3. Backend:
   - Genera token único con timestamp
   - Lo asocia al usuario en BD
   - Envía email con enlace:
     ```
     http://127.0.0.1:4200/reset-password?email=XXX&token=YYY
     ```
4. Usuario hace clic en enlace
5. Frontend carga formulario de nueva contraseña
6. Usuario ingresa nueva contraseña (2 veces para confirmar)
7. Backend:
   - Valida token y que no haya expirado
   - Marca token como usado
   - Encripta nueva contraseña
   - Actualiza usuario en BD

**Seguridad:**
- Tokens de un solo uso
- Expiración de 24 horas
- Encriptación SHA-256 de contraseñas

**Código relevante:**
- [forgot-password.ts](gramolaJavier/gramolaJa/src/app/forgot-password/forgot-password.ts) - Solicitud
- [reset-password.ts](gramolaJavier/gramolaJa/src/app/reset-password/reset-password.ts) - Cambio

### 7. Firma Digital

**Implementación:**
- Canvas HTML5 para capturar trazos
- Eventos táctiles y de ratón
- Conversión a imagen base64 PNG
- Almacenamiento en BD como TEXT
- Visualización en dashboard

**Código relevante:**
- [register.ts](gramolaJavier/gramolaJa/src/app/register/register.ts) - Líneas 150-250 (canvas)
- [dashboard.html](gramolaJavier/gramolaJa/src/app/dashboard/dashboard.html) - Visualización de firma

## Estructura de Base de Datos

### Tabla: users
```sql
CREATE TABLE users (
    email VARCHAR(255) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,      -- SHA-256
    bar_name VARCHAR(255) NOT NULL,
    client_id VARCHAR(255),
    client_secret VARCHAR(255),
    account_confirmed BOOLEAN DEFAULT FALSE,
    payment_confirmed BOOLEAN DEFAULT FALSE,
    token_id VARCHAR(36),
    address VARCHAR(500),
    latitude DOUBLE,
    longitude DOUBLE,
    signature TEXT                       -- Base64 PNG
);
```

### Tabla: prices
```sql
CREATE TABLE prices (
    code VARCHAR(50) PRIMARY KEY,
    cents BIGINT NOT NULL,
    description VARCHAR(255),
    euros DOUBLE
);
```

Valores iniciales:
- `SUBSCRIPTION_MONTHLY`: 499 céntimos (4.99€)
- `QUEUE_NEW`: 299 céntimos (2.99€)
- `QUEUE_ADVANCE`: 199 céntimos (1.99€)

### Tabla: spotify_tokens
```sql
CREATE TABLE spotify_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    access_token TEXT,
    refresh_token TEXT,
    expires_at TIMESTAMP
);
```

### Tabla: queue_payment_transactions
```sql
CREATE TABLE queue_payment_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255),
    payment_intent_id VARCHAR(255) UNIQUE,
    customer_name VARCHAR(255),
    track_uri VARCHAR(255),
    track_name VARCHAR(255),
    artist_name VARCHAR(255),
    album_name VARCHAR(255),
    price_cents BIGINT,
    status VARCHAR(50),                  -- completed, failed, pending
    created_at TIMESTAMP,
    device_id VARCHAR(255)
);
```

## Testing

### Tests Unitarios

Los tests unitarios se ejecutan con:
```bash
mvn test
```

### Tests E2E con Selenium

**Escenario 1: Pago exitoso**
1. Login con cuenta de test
2. Buscar canción "Hola Perdida"
3. Hacer clic en "Añadir" (2.99€)
4. Ingresar nombre "Javier Test"
5. Ingresar tarjeta de prueba: 4242 4242 4242 4242
6. Confirmar pago
7. Verificar en BD:
   - Existe transacción con status "completed"
   - Nombre del cliente coincide
   - Nombre de canción y artista guardados
   - Precio es 299 céntimos

**Escenario 2: Pago fallido**
1. Navegar a dashboard (sesión ya iniciada)
2. Buscar canción "Sweet"
3. Hacer clic en "Añadir"
4. Ingresar nombre "Cliente Error"
5. Ingresar tarjeta que falla: 4000 0000 0000 0002
6. Intentar confirmar pago
7. Verificar:
   - Se muestra mensaje de error
   - NO se crea transacción "completed" en BD

**Tarjetas de prueba Stripe:**
- `4242 4242 4242 4242` - Pago exitoso
- `4000 0000 0000 0002` - Tarjeta rechazada
- `4000 0000 0000 9995` - Fondos insuficientes

**Ejecutar tests:**
```bash
mvn test -Dtest=QueuePaymentSeleniumTest
```

**Requisitos:**
- Chrome instalado
- Backend corriendo en http://localhost:8080
- Frontend corriendo en http://127.0.0.1:4200
- Cuenta de test creada y configurada (con email confirmado y pago completado)

**Código:**
- [QueuePaymentSeleniumTest.java](gramolaJavier/src/test/java/edu/uclm/esi/gramolaJavier/QueuePaymentSeleniumTest.java)

## Instalación y Configuración

### Prerrequisitos

- Java 21 o superior
- Node.js 18 o superior
- MySQL 8.0 o superior
- Maven 3.8+
- Cuenta de Spotify Developer
- Cuenta de Stripe

### 1. Clonar el Repositorio

```bash
git clone https://github.com/usuario/gramolaJavier.git
cd gramolaJavier
```

### 2. Configurar Base de Datos

```bash
mysql -u root -p
CREATE DATABASE gramola;
USE gramola;
SOURCE sql_scripts/schema.sql;
```

### 3. Configurar Backend

Editar `gramolaJavier/src/main/resources/application.properties`:

```properties
# Base de datos
spring.datasource.url=jdbc:mysql://localhost:3306/gramola
spring.datasource.username=root
spring.datasource.password=tu_password

# Stripe (obtener de https://dashboard.stripe.com/test/apikeys)
stripe.secret.key=sk_test_tu_clave_secreta

# Email (Gmail)
spring.mail.username=tu_email@gmail.com
spring.mail.password=tu_app_password

# Spotify redirect
spotify.redirect.uri=http://127.0.0.1:4200/callback
```

**Compilar:**
```bash
cd gramolaJavier
mvn clean install
```

**Ejecutar:**
```bash
mvn spring-boot:run
```

El backend estará disponible en: http://localhost:8080

### 4. Configurar Frontend

```bash
cd gramolaJavier/gramolaJa
npm install
```

**Ejecutar:**
```bash
ng serve --host 127.0.0.1
```

El frontend estará disponible en: http://127.0.0.1:4200

### 5. Configurar Spotify Developer

1. Ir a https://developer.spotify.com/dashboard
2. Crear nueva aplicación
3. Añadir Redirect URI: `http://127.0.0.1:4200/callback`
4. Copiar Client ID y Client Secret
5. Al registrarse en la app, usar esas credenciales

### 6. Configurar Stripe

1. Ir a https://dashboard.stripe.com/test/apikeys
2. Copiar Secret Key (empieza con `sk_test_`)
3. Pegar en `application.properties`
4. Para pruebas, usar tarjeta 4242 4242 4242 4242

## Uso de la Aplicación

### Para Propietarios de Bares

1. **Registro:**
   - Ir a http://127.0.0.1:4200/register
   - Completar todos los campos
   - Dibujar firma en el canvas
   - Hacer clic en "Registrarse"

2. **Confirmación:**
   - Revisar email
   - Hacer clic en enlace de confirmación
   - Serás redirigido a pago

3. **Pago de Suscripción:**
   - Ingresar datos de tarjeta
   - Completar pago de 4.99€
   - Serás redirigido a Spotify

4. **Autorización Spotify:**
   - Hacer clic en "Aceptar"
   - Serás redirigido al dashboard

5. **Usar Dashboard:**
   - Seleccionar dispositivo Spotify
   - Buscar y reproducir música
   - Ver cola en tiempo real
   - Gestionar playlists

### Para Clientes del Bar

1. **Acceso:**
   - El propietario comparte la URL del dashboard
   - O tiene una tablet/móvil disponible en el bar

2. **Buscar Canción:**
   - Usar el buscador
   - Ver resultados con caratulas

3. **Pagar y Añadir:**
   - Hacer clic en "Añadir" (2.99€)
   - Ingresar nombre
   - Permitir acceso a ubicación
   - Ingresar datos de tarjeta
   - Confirmar pago

4. **Disfrutar:**
   - La canción se añade automáticamente a la cola
   - Aparecerá en la lista de reproducción
   - Sonará en el dispositivo del bar

## Problemas Comunes y Soluciones

### Backend

**Error: "Access denied for user 'root'@'localhost'"**
- Verificar usuario y contraseña en `application.properties`
- Asegurarse de que MySQL esté corriendo: `systemctl status mysql`

**Error: "Spotify rejected the request"**
- Verificar que Client ID y Secret sean correctos
- Confirmar que Redirect URI esté configurado en Spotify Dashboard
- Debe ser exactamente: `http://127.0.0.1:4200/callback`

**Error: "Stripe API key invalid"**
- Verificar que la clave empiece con `sk_test_`
- No usar la clave pública (`pk_test_`)
- Copiar de https://dashboard.stripe.com/test/apikeys

### Frontend

**Error: "Cannot GET /"**
- Verificar que el comando sea: `ng serve --host 127.0.0.1`
- El puerto por defecto es 4200
- Si está ocupado, usar: `ng serve --host 127.0.0.1 --port 4201`

**Error: CORS al hacer peticiones**
- Verificar que `CorsConfig.java` tenga: `http://127.0.0.1:4200`
- No usar `http://localhost:4200` (diferente origen)

**Spotify no redirige correctamente**
- Verificar que la URL en `login.ts` sea: `http://127.0.0.1:4200/callback`
- Debe coincidir exactamente con Spotify Dashboard

### Tests de Selenium

**Error: "ChromeDriver not found"**
- WebDriverManager descarga automáticamente ChromeDriver
- Asegurarse de tener Chrome instalado
- Si falla, descargar manualmente de: https://chromedriver.chromium.org/

**Test falla en login**
- Verificar que la cuenta de test exista en la base de datos
- Verificar que esté confirmada y pagada
- Verificar que las credenciales sean correctas en QueuePaymentSeleniumTest.java

**Test falla en búsqueda de canción**
- Verificar que haya conexión a Internet
- Verificar que Spotify API esté funcionando
- Verificar que los tokens no hayan expirado

## Mantenimiento y Mejoras Futuras

### Mejoras Potenciales

1. **Sistema de votación:**
   - Permitir que clientes voten por canciones en la cola
   - Reordenar cola según votos

2. **Historial de reproducciones:**
   - Guardar qué canciones se han reproducido
   - Estadísticas para el propietario
   - Top 10 canciones más pedidas

3. **Sistema de descuentos:**
   - Happy hour (precios reducidos en ciertos horarios)
   - Programa de fidelidad
   - Códigos promocionales

4. **Moderación:**
   - Lista negra de canciones
   - Tiempo máximo de canción
   - Límite de canciones por cliente

5. **Multi-bar:**
   - Un propietario gestiona varios bares
   - Dashboard centralizado
   - Reportes consolidados

6. **App móvil nativa:**
   - iOS y Android
   - Notificaciones push
   - Escaneo de QR para acceso rápido

### Mantenimiento Regular

1. **Actualizar dependencias:**
   ```bash
   mvn versions:display-dependency-updates
   npm outdated
   ```

2. **Backup de base de datos:**
   ```bash
   mysqldump -u root -p gramola > backup_$(date +%Y%m%d).sql
   ```

3. **Monitorear logs:**
   - Backend: `logs/spring-boot-application.log`
   - Frontend: Consola del navegador
   - Stripe: https://dashboard.stripe.com/test/logs
   - Spotify: https://developer.spotify.com/dashboard

4. **Renovar tokens:**
   - Spotify tokens se renuevan automáticamente
   - Revisar que el refresh funcione correctamente

## Créditos y Licencia

- **Autor:** Javier Alcaide Cea
- **GitHub:** https://github.com/Jaavii004
- **Gmail:** alcaidejavier6@gmail.com
- **Universidad:** UCLM - Escuela Superior de Informática
- **Asignatura:** Tecnología y Sistemas Web
- **Curso:** 2024/2025

### Licencia

Este proyecto es de código educativo. No está permitido su uso comercial sin autorización.

### APIs y Servicios Utilizados

- **Spotify Web API:** https://developer.spotify.com/documentation/web-api/
- **Stripe API:** https://stripe.com/docs/api
- **Nominatim (OpenStreetMap):** https://nominatim.org/release-docs/develop/api/Overview/
- **JavaMail:** https://javaee.github.io/javamail/

---

## Contacto y Soporte

Para dudas o problemas:
- GitHub Issues: [Crear issue](https://github.com/Jaavii004/gramolaJavier/issues)

---

**Última actualización:** 16 de enero de 2025
