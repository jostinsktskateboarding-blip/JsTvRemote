# Js TV Remote 1.3.0

Versión preparada para subir al repositorio y compilar con GitHub Actions.

## Qué se corrigió
- `AndroidTvRemote.kt` reescrito para evitar el error `X.509 not found` y el error de `X509v3CertificateBuilder`.
- Operaciones de red movidas a `Dispatchers.IO` para evitar bloqueos de la interfaz.
- Emparejamiento Android TV conservado con código hexadecimal de 6 caracteres.
- Workflow de GitHub Actions limpio: no usa secretos dentro de condiciones inválidas.
- El workflow renombra automáticamente `app 2` a `app` si esa carpeta vuelve a aparecer.
- Genera APK Debug, APK Release y AAB Release.
- Interfaz renovada con estilo oscuro, minimalista, tarjetas y controles más modernos.
- Versión de app: 1.3.0 (versionCode 4).

## GitHub Actions
Al hacer push a `main`, la acción ejecuta lint y compila:
- `app-debug.apk`
- `app-release-unsigned.apk` si no hay firma configurada
- `app-release.apk` si hay firma configurada
- `app-release.aab` si hay firma configurada o sin firmar en caso contrario

Los archivos aparecen en **Actions > Js TV Remote - Android Build > Artifacts**.

## Importante
La carpeta del módulo debe llamarse `app`. El workflow corrige automáticamente el caso `app 2` durante CI, pero lo recomendable es subirla como `app` desde el principio.
