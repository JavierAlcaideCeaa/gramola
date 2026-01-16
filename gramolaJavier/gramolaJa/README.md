# GramolaJa

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 20.3.3.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Karma](https://karma-runner.github.io) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.

# 🎵 Instrucciones para Obtener Credenciales de Spotify

## Paso 1: Acceder a Spotify for Developers

1. Abre tu navegador y ve a: https://developer.spotify.com/dashboard
2. Inicia sesión con tu cuenta de Spotify (la del bar)
3. Si no tienes cuenta, créala desde https://www.spotify.com/signup

## Paso 2: Crear una Nueva Aplicación

1. En el Dashboard, haz clic en **"Create app"** (Crear aplicación)
2. Rellena el formulario:
   - **App name**: `Gramola [Nombre de tu bar]`
   - **App description**: `Sistema de gramola interactiva para mi bar`
   - **Website**: Deja en blanco o pon tu web si tienes
   - **Redirect URIs**: `http://127.0.0.1:4200/callback`
   - **APIs used**: Marca las siguientes opciones:
     - ✅ Web API
     - ✅ Web Playback SDK

3. Acepta los términos de servicio
4. Haz clic en **"Save"** (Guardar)

## Paso 3: Obtener las Credenciales

Una vez creada la aplicación:

1. En el Dashboard de tu aplicación, verás:
   - **Client ID**: Un código alfanumérico largo
   - **Client Secret**: Está oculto por defecto

2. **Para ver el Client Secret**:
   - Haz clic en **"Show client secret"**
   - Copia ambos valores y guárdalos en un lugar seguro

### ⚠️ IMPORTANTE

- **Client ID**: Es público, puedes compartirlo
- **Client Secret**: Es como una contraseña, NO lo compartas con nadie
- **Guarda ambos valores**: Los necesitarás para registrarte en la gramola

## Paso 4: Configurar Redirect URIs

1. En la configuración de tu app en Spotify, busca **"Redirect URIs"**
2. Añade: `http://127.0.0.1:4200/callback`
3. Haz clic en **"Add"** y luego en **"Save"**

## Paso 5: Registrarse en la Gramola

Con tus credenciales listas:

1. Ve a la página de registro de la gramola
2. Rellena todos los campos:
   - Nombre del bar
   - Correo electrónico
   - Contraseña (y confirmación)
   - **Spotify Client ID** (el que acabas de copiar)
   - **Spotify Client Secret** (el que acabas de copiar)

3. Haz clic en **"Registrarse"**
4. Revisa tu correo para confirmar tu cuenta
5. Completa el pago
6. ¡Listo! Ya puedes usar la gramola

## 🆘 ¿Problemas?

- Si olvidas tus credenciales, siempre puedes verlas en el Dashboard de Spotify
- Si necesitas regenerar el Client Secret, puedes hacerlo desde el Dashboard
- Asegúrate de que la Redirect URI sea exactamente: `http://127.0.0.1:4200/callback`

## 📞 Soporte

Si tienes problemas técnicos, contacta con el equipo de soporte de la gramola.