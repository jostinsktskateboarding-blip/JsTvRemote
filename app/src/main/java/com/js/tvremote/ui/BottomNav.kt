package com.js.tvremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.js.tvremote.ui.theme.TvRemoteSurface
import com.js.tvremote.ui.theme.TvRemoteTextPrimary
import com.js.tvremote.ui.theme.TvRemoteTextSecondary

enum class RemoteTab { REMOTO, APPS, AJUSTES }

@Composable
fun RemoteBottomNav(
    selected: RemoteTab,
    onSelectRemoto: () -> Unit,
    onSelectApps: () -> Unit,
    onSelectAjustes: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(TvRemoteSurface)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomNavItem(
            label = "Remoto",
            icon = Icons.Filled.SettingsRemote,
            selected = selected == RemoteTab.REMOTO,
            onClick = onSelectRemoto
        )
        BottomNavItem(
            label = "Apps",
            icon = Icons.Filled.Apps,
            selected = selected == RemoteTab.APPS,
            onClick = onSelectApps
        )
        BottomNavItem(
            label = "Ajustes",
            icon = Icons.Filled.Settings,
            selected = selected == RemoteTab.AJUSTES,
            onClick = onSelectAjustes
        )
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color(0xFF2C2C2E) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) TvRemoteTextPrimary else TvRemoteTextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            label,
            color = if (selected) TvRemoteTextPrimary else TvRemoteTextSecondary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
