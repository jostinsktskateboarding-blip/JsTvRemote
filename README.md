# Js TV Remote — Android

Control remoto para televisores compatibles mediante WiFi local. Proyecto Android nativo con Kotlin + Jetpack Compose.

## Compatibilidad implementada
- Roku / Roku TV mediante ECP
- Samsung Smart TV mediante WebSocket (8001/8002)
- LG webOS mediante SSAP + pointer input socket y emparejamiento
- Descubrimiento SSDP/UPnP en red local

Los dispositivos detectados como genéricos o DLNA se muestran, pero no se envían comandos Roku incorrectamente: se marcan como no compatibles con el control remoto actual.

## Google Play
- `minSdk`: 23
- `targetSdk`: 36 (requisito actual para nuevas apps y actualizaciones)
- `compileSdk`: 36
- AAB recomendado para publicación
- AdMob integrado con UMP para consentimiento
- Política de privacidad incluida en `privacy-policy.html`

## Importante antes de publicar
1. Publica `privacy-policy.html` en una URL HTTPS activa y coloca esa URL en la ficha de Play Console y en cualquier configuración correspondiente.
2. Completa Data safety de acuerdo con la implementación real y la versión de AdMob utilizada.
3. Crea una clave de firma de release y configura `signingConfig`/GitHub Secrets para generar el AAB firmado.
4. Prueba la app en dispositivos reales con Roku, Samsung y LG antes de enviar a revisión.

## Build local
Usa Android Studio con JDK 17 y Android SDK 36.


## Android TV / Google TV Remote v2

This version adds standard `_androidtvremote2._tcp` mDNS discovery and the Android TV Remote v2 TLS/protobuf transport (ports 6467 for first-time pairing and 6466 for remote commands). The phone connects directly to the TV on the local Wi-Fi network; no application server is required.

First connection: select the TV, press a remote key, wait for the 6-character hexadecimal code shown on the TV, and enter it in the app. The client certificate is persisted locally so subsequent connections can reuse the pairing identity.

The existing colors and remote icons are preserved. The activity uses edge-to-edge rendering so the UI can use the full phone display.
