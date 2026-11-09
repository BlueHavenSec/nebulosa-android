# Nébula para Android

Primera versión nativa de Nébula, una app de streaming con catálogo demo, búsqueda local y reproducción mediante embeds de VidSrc.

## Abrir y compilar

1. Abre esta carpeta en Android Studio.
2. Espera a que Gradle sincronice el proyecto.
3. Ejecuta la variante `debug` en un dispositivo o emulador con Android 7.0 o superior.

El APK de depuración queda en `app/build/outputs/apk/debug/app-debug.apk` después de compilar.

## Integración VidSrc

La app construye estas URLs en `MainActivity.java`:

- Películas: `https://vidsrc.sh/embed/movie/{tmdbId}`
- Series: `https://vidsrc.sh/embed/tv/{tmdbId}/{season}/{episode}`

El catálogo es una base demo con IDs de TMDB. Para una app de producción, sustituye el catálogo por una fuente de metadatos con licencia y confirma que el proveedor de video y el contenido se pueden distribuir en los territorios objetivo.

## TMDB

Nébula usa `movie/popular` y `tv/popular` de TMDB para títulos, años, sinopsis y `poster_path`. Las carátulas se cargan desde `image.tmdb.org`. Añade tu clave de TMDB en `local.properties`:

```properties
TMDB_API_KEY=tu_clave_de_tmdb
```

La clave se convierte en `BuildConfig.TMDB_API_KEY` durante la compilación. Si está vacía, la app conserva el catálogo demo local. TMDB requiere atribución; por eso Nébula muestra el aviso correspondiente en la pantalla principal. [Guía oficial de autenticación de TMDB](https://developer.themoviedb.org/docs/authentication-application)

## Filtro de anuncios

El WebView incluye un filtro integrado para dominios publicitarios conocidos, popunders, banners externos y ventanas nuevas. El filtro vive en `AdBlockingWebViewClient` dentro de `MainActivity.java` y se puede ampliar con más dominios si aparece algún proveedor nuevo.
