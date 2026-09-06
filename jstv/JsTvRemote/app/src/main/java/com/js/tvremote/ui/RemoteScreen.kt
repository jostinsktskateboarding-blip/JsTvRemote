package com.js.tvremote.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SubdirectoryArrowLeft
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.js.tvremote.net.Brand
import com.js.tvremote.net.LgWebOsController
import com.js.tvremote.net.RokuController
import com.js.tvremote.net.SamsungController
import com.js.tvremote.net.TvDevice
import com.js.tvremote.net.AndroidTvRemote
import com.js.tvremote.ui.theme.TvRemoteOkBackground
import com.js.tvremote.ui.theme.TvRemoteOkBorder
import com.js.tvremote.ui.theme.TvRemotePill
import com.js.tvremote.ui.theme.TvRemotePower
import com.js.tvremote.ui.theme.TvRemotePurpleDark
import com.js.tvremote.ui.theme.TvRemotePurpleLight
import com.js.tvremote.ui.theme.TvRemoteSurface
import com.js.tvremote.ui.theme.TvRemoteTextPrimary
import com.js.tvremote.ui.theme.TvRemoteTextSecondary
import kotlinx.coroutines.launch
import kotlinx.coroutines.CompletableDeferred

@Composable
fun RemoteScreen(
    device: TvDevice,
    onBack: () -> Unit,
    onCommandSent: () -> Unit,
    onOpenApps: () -> Unit = {},
    onOpenAjustes: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var lgController by remember { mutableStateOf<LgWebOsController?>(null) }
    var status by remember { mutableStateOf("Listo") }
    var androidController by remember { mutableStateOf<AndroidTvRemote?>(null) }
    var showPairDialog by remember { mutableStateOf(false) }
    var pinDeferred by remember { mutableStateOf<CompletableDeferred<String>?>(null) }

    DisposableEffect(device) {
        if (device.brand == Brand.ANDROID_TV) {
            androidController = AndroidTvRemote(context, device)
            status = if (androidController!!.isPaired()) "Listo para conectar" else "Emparejamiento requerido"
        }
        if (device.brand == Brand.LG_WEBOS) {
            val prefs = context.getSharedPreferences("tv_remote", Context.MODE_PRIVATE)
            val savedKey = prefs.getString("lg_key_${device.ip}", null)
            val controller = LgWebOsController(
                ip = device.ip,
                onReady = { status = "Conectado" },
                onClientKey = { key -> prefs.edit().putString("lg_key_${device.ip}", key).apply() },
                onError = { message -> status = message }
            )
            lgController = controller
            controller.connect(savedKey)
        }
        onDispose { lgController?.close(); androidController?.close(); pinDeferred?.cancel() }
    }

    fun sendKey(action: String) {
        scope.launch {
            when (device.brand) {
                Brand.ANDROID_TV -> {
                    val controller = androidController ?: return@launch
                    val key = when (action) {
                        "HOME" -> 3; "BACK" -> 4; "UP" -> 19; "DOWN" -> 20; "LEFT" -> 21; "RIGHT" -> 22;
                        "OK" -> 23; "PLAY" -> 85; "REWIND" -> 89; "FORWARD" -> 90; "VOL_UP" -> 24; "VOL_DOWN" -> 25;
                        "MUTE" -> 164; "POWER" -> 26; else -> return@launch
                    }
                    var result = if (controller.isPaired()) controller.sendKey(key) else Result.failure(Exception("not paired"))
                    if (result.isFailure) {
                        status = "Conectando…"
                        val deferred = CompletableDeferred<String>()
                        pinDeferred = deferred
                        val pairResult = controller.pairWithPrompt {
                            showPairDialog = true
                            deferred.await()
                        }
                        showPairDialog = false
                        pinDeferred = null
                        if (pairResult.isSuccess) {
                            status = "Conectado"
                            result = controller.sendKey(key)
                        } else status = pairResult.exceptionOrNull()?.message ?: "No se pudo emparejar"
                    }
                    if (result.isSuccess) onCommandSent() else if (result.exceptionOrNull()?.message != "not paired") status = result.exceptionOrNull()?.message ?: "Error de conexión"
                }
                Brand.ROKU -> {
                    val key = when (action) {
                        "HOME" -> RokuController.Key.HOME
                        "BACK" -> RokuController.Key.BACK
                        "UP" -> RokuController.Key.UP
                        "DOWN" -> RokuController.Key.DOWN
                        "LEFT" -> RokuController.Key.LEFT
                        "RIGHT" -> RokuController.Key.RIGHT
                        "OK" -> RokuController.Key.SELECT
                        "PLAY" -> RokuController.Key.PLAY
                        "REWIND" -> RokuController.Key.REWIND
                        "FORWARD" -> RokuController.Key.FORWARD
                        "VOL_UP" -> RokuController.Key.VOLUME_UP
                        "VOL_DOWN" -> RokuController.Key.VOLUME_DOWN
                        "MUTE" -> RokuController.Key.MUTE
                        "POWER" -> RokuController.Key.POWER
                        else -> return@launch
                    }
                    RokuController.send(device.ip, key)
                    onCommandSent()
                }
                Brand.SAMSUNG -> {
                    val key = when (action) {
                        "HOME" -> SamsungController.Key.HOME
                        "BACK" -> SamsungController.Key.BACK
                        "UP" -> SamsungController.Key.UP
                        "DOWN" -> SamsungController.Key.DOWN
                        "LEFT" -> SamsungController.Key.LEFT
                        "RIGHT" -> SamsungController.Key.RIGHT
                        "OK" -> SamsungController.Key.ENTER
                        "PLAY" -> SamsungController.Key.PLAY
                        "REWIND" -> SamsungController.Key.REWIND
                        "FORWARD" -> SamsungController.Key.FORWARD
                        "VOL_UP" -> SamsungController.Key.VOLUME_UP
                        "VOL_DOWN" -> SamsungController.Key.VOLUME_DOWN
                        "MUTE" -> SamsungController.Key.MUTE
                        "POWER" -> SamsungController.Key.POWER
                        else -> return@launch
                    }
                    SamsungController.connectAndSend(device.ip, "Js TV Remote", key)
                    onCommandSent()
                }
                Brand.LG_WEBOS -> {
                    val button = when (action) {
                        "HOME" -> "HOME"
                        "BACK" -> "BACK"
                        "UP" -> "UP"
                        "DOWN" -> "DOWN"
                        "LEFT" -> "LEFT"
                        "RIGHT" -> "RIGHT"
                        "OK" -> "ENTER"
                        "PLAY" -> "PLAY"
                        "REWIND" -> "REWIND"
                        "FORWARD" -> "FASTFORWARD"
                        "VOL_UP" -> "VOLUMEUP"
                        "VOL_DOWN" -> "VOLUMEDOWN"
                        "MUTE" -> "MUTE"
                        "POWER" -> "POWER"
                        else -> return@launch
                    }
                    if (lgController?.sendButton(button) == true) {
                        onCommandSent()
                    } else {
                        status = "Acepta la conexión en el TV LG y vuelve a intentar."
                    }
                }
                Brand.GENERIC_DLNA, Brand.UNKNOWN -> {
                    status = "Este dispositivo fue detectado, pero no tiene un protocolo de control compatible."
                }
            }
        }
    }

    if (showPairDialog) {
        AlertDialog(
            onDismissRequest = { pinDeferred?.cancel() },
            title = { Text("Emparejar TV") },
            text = { Text("Mira la pantalla del televisor. Introduce aquí el código de 6 caracteres que aparece en el TV.") },
            confirmButton = {
                var pin by remember { mutableStateOf("") }
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = pin, onValueChange = { pin = it.uppercase().take(6) },
                        label = { Text("Código") }, singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { if (pin.length == 6) pinDeferred?.complete(pin) }) { Text("Conectar") }
                }
            },
            dismissButton = { TextButton(onClick = { pinDeferred?.cancel() }) { Text("Cancelar") } }
        )
    }

    Scaffold(containerColor = Color.Black) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Black)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Fila superior: power, nombre del TV, cast
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleIconButton(
                    icon = Icons.Default.PowerSettingsNew,
                    contentDescription = "Encendido",
                    background = TvRemoteSurface,
                    tint = TvRemotePower,
                    onClick = { sendKey("POWER") }
                )
                Text(
                    device.name,
                    color = TvRemoteTextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                CircleIconButton(
                    icon = Icons.Default.Cast,
                    contentDescription = "Transmitir",
                    background = TvRemoteSurface,
                    tint = TvRemoteTextPrimary,
                    onClick = { /* Envío por cast: próxima versión */ }
                )
            }

            Spacer(Modifier.height(24.dp))

            // Fila de accesos: Atrás / Apps / Inicio
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PillIconButton(Icons.Default.SubdirectoryArrowLeft, "Atrás", Modifier.weight(1f)) { sendKey("BACK") }
                PillIconButton(Icons.Default.Apps, "Apps", Modifier.weight(1f)) { onOpenApps() }
                PillIconButton(Icons.Default.Home, "Inicio", Modifier.weight(1f)) { sendKey("HOME") }
            }

            Spacer(Modifier.height(28.dp))

            // Cruz direccional con OK al centro
            DirectionalPad(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onUp = { sendKey("UP") },
                onDown = { sendKey("DOWN") },
                onLeft = { sendKey("LEFT") },
                onRight = { sendKey("RIGHT") },
                onOk = { sendKey("OK") }
            )

            Spacer(Modifier.height(28.dp))

            // Fila multimedia: retroceder / play / adelantar
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PillIconButton(Icons.Default.FastRewind, "Retroceder", Modifier.weight(1f)) { sendKey("REWIND") }
                PillIconButton(Icons.Default.PlayArrow, "Reproducir", Modifier.weight(1f)) { sendKey("PLAY") }
                PillIconButton(Icons.Default.FastForward, "Adelantar", Modifier.weight(1f)) { sendKey("FORWARD") }
            }

            Spacer(Modifier.height(10.dp))

            // Fila de volumen: mute / vol- / vol+
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PillIconButton(Icons.Default.VolumeOff, "Silenciar", Modifier.weight(1f)) { sendKey("MUTE") }
                PillIconButton(Icons.Default.VolumeDown, "Bajar volumen", Modifier.weight(1f)) { sendKey("VOL_DOWN") }
                PillIconButton(Icons.Default.VolumeUp, "Subir volumen", Modifier.weight(1f)) { sendKey("VOL_UP") }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                status,
                color = TvRemoteTextSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            RemoteBottomNav(
                selected = RemoteTab.REMOTO,
                onSelectRemoto = {},
                onSelectApps = onOpenApps,
                onSelectAjustes = onOpenAjustes
            )

            Spacer(Modifier.height(8.dp))

            BannerAdView()
        }
    }
}

@Composable
private fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    background: Color,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PillIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TvRemotePill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = TvRemoteTextPrimary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DirectionalPad(
    modifier: Modifier = Modifier,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onOk: () -> Unit
) {
    val armBrush = Brush.linearGradient(listOf(TvRemotePurpleLight, TvRemotePurpleDark))
    val armSize = 200.dp
    val armThickness = 68.dp

    Box(
        modifier = modifier.size(armSize),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(armThickness)
                .fillMaxHeight()
                .clip(RoundedCornerShape(34.dp))
                .background(armBrush)
        )
        Box(
            modifier = Modifier
                .height(armThickness)
                .fillMaxWidth()
                .clip(RoundedCornerShape(34.dp))
                .background(armBrush)
        )

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .width(armThickness)
                .height((armSize - armThickness) / 2)
                .clickable(onClick = onUp)
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .width(armThickness)
                .height((armSize - armThickness) / 2)
                .clickable(onClick = onDown)
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .height(armThickness)
                .width((armSize - armThickness) / 2)
                .clickable(onClick = onLeft)
        )
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .height(armThickness)
                .width((armSize - armThickness) / 2)
                .clickable(onClick = onRight)
        )

        Icon(
            Icons.Default.KeyboardArrowUp, contentDescription = "Arriba", tint = TvRemoteTextPrimary,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp).size(22.dp)
        )
        Icon(
            Icons.Default.KeyboardArrowDown, contentDescription = "Abajo", tint = TvRemoteTextPrimary,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).size(22.dp)
        )
        Icon(
            Icons.Default.KeyboardArrowLeft, contentDescription = "Izquierda", tint = TvRemoteTextPrimary,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp).size(22.dp)
        )
        Icon(
            Icons.Default.KeyboardArrowRight, contentDescription = "Derecha", tint = TvRemoteTextPrimary,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp).size(22.dp)
        )

        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(CircleShape)
                .background(TvRemoteOkBackground)
                .border(1.5.dp, TvRemoteOkBorder, CircleShape)
                .clickable(onClick = onOk),
            contentAlignment = Alignment.Center
        ) {
            Text("OK", color = TvRemoteTextPrimary, style = MaterialTheme.typography.labelLarge)
        }
    }
}
