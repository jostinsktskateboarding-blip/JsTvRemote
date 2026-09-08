package com.js.tvremote.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.js.tvremote.net.SsdpDiscovery
import com.js.tvremote.net.TvDevice
import kotlinx.coroutines.launch

@Composable
fun DiscoverScreen(onDeviceSelected: (TvDevice) -> Unit, onAboutClick: () -> Unit) {
    val context = LocalContext.current
    var devices by remember { mutableStateOf<List<TvDevice>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun search() {
        scope.launch {
            searching = true
            devices = SsdpDiscovery.discover(context)
            searching = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Discovery still runs after denial so older/less-restricted Android versions remain usable.
        search()
    }

    fun startSearch() {
        if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            search()
        }
    }

    LaunchedEffect(Unit) { startSearch() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Js TV Remote") },
                actions = { TextButton(onClick = onAboutClick) { Text("Acerca de") } }
            )
        },
        bottomBar = { BannerAdView() }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Televisores en tu red WiFi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Button(onClick = { startSearch() }, enabled = !searching) {
                    Text(if (searching) "Buscando..." else "Buscar")
                }
            }

            Spacer(Modifier.height(12.dp))

            if (searching) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            if (!searching && devices.isEmpty()) {
                Text(
                    "No se encontró ningún TV. Asegúrate de que el celular y el televisor estén en la MISMA red WiFi y que el TV esté encendido.",
                    modifier = Modifier.padding(top = 24.dp)
                )
            }

            LazyColumn(Modifier.fillMaxSize()) {
                items(devices, key = { "${it.ip}-${it.brand}" }) { device ->
                    ListItem(
                        headlineContent = { Text(device.name) },
                        supportingContent = { Text("${device.ip} · ${brandLabel(device.brand)}") },
                        modifier = Modifier.clickable { onDeviceSelected(device) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun brandLabel(brand: com.js.tvremote.net.Brand): String = when (brand) {
    com.js.tvremote.net.Brand.ANDROID_TV -> "Android TV / Google TV"
    com.js.tvremote.net.Brand.ROKU -> "Roku"
    com.js.tvremote.net.Brand.LG_WEBOS -> "LG webOS"
    com.js.tvremote.net.Brand.SAMSUNG -> "Samsung"
    com.js.tvremote.net.Brand.GENERIC_DLNA -> "DLNA"
    com.js.tvremote.net.Brand.UNKNOWN -> "Dispositivo"
}
