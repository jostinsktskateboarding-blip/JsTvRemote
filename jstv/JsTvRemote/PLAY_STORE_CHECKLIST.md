# Google Play — checklist de publicación

## Estado técnico del proyecto
- [x] Proyecto Android nativo (Kotlin/Compose).
- [x] `compileSdk = 36`.
- [x] `targetSdk = 36`.
- [x] `minSdk = 23`.
- [x] `applicationId = com.js.tvremote`.
- [x] `versionCode = 2`, `versionName = 1.1.0`.
- [x] `android:exported=true` en la actividad principal.
- [x] Ícono nuevo instalado como launcher y round icon.
- [x] Permiso `NEARBY_WIFI_DEVICES` para Android 13+.
- [x] SSDP usa `MulticastLock` para mejorar el descubrimiento en WiFi.
- [x] Roku ECP.
- [x] Samsung WebSocket 8001 con fallback 8002.
- [x] LG webOS SSAP + pointer input socket + guardado de client-key.
- [x] Dispositivos genéricos/DLNA ya no reciben comandos Roku por error.
- [x] AdMob actualizado a 25.4.0.
- [x] UMP 4.0.0 para gestión de consentimiento.
- [x] Política de privacidad dentro de la app y archivo web incluido.

## Antes de enviar a producción
1. Publica `privacy-policy.html` en una URL HTTPS pública y reemplaza el valor de `privacy_policy_url` si vas a utilizarlo desde la app.
2. Completa el formulario Data safety de Play Console según la configuración final de AdMob.
3. Declara que la app utiliza publicidad.
4. Configura clasificación de contenido y público objetivo. La app no está diseñada específicamente para niños.
5. Genera un AAB firmado con una clave de release. No subas la clave ni contraseñas al repositorio.
6. Prueba en teléfonos Android reales y, como mínimo, con un Roku, Samsung y LG webOS disponibles en la misma red WiFi.
7. Prueba el rechazo del permiso de dispositivos cercanos y confirma que la app explica correctamente que necesita acceso a la red local.
8. Prueba el primer emparejamiento de LG y que el client-key se conserve después de cerrar y abrir la app.
9. Comprueba anuncios con dispositivos de prueba de AdMob; nunca hagas clic en anuncios reales propios.
10. Si la cuenta de Play es personal y fue creada después del 13/11/2023, prepara una prueba cerrada con al menos 12 testers durante 14 días antes de solicitar acceso a producción.
11. Completa la verificación de desarrollador y el registro del nombre de paquete de acuerdo con los plazos vigentes de Android.

## Data safety — base para revisar
El código de la app no tiene servidor propio ni cuenta de usuario. Sin embargo, AdMob sí recopila/ comparte automáticamente datos como IP, interacciones con el producto, diagnósticos y determinados identificadores del dispositivo. La declaración final debe reflejar exactamente la configuración de AdMob que esté activa en Play Console.
