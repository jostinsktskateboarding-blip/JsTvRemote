package com.js.tvremote

import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.js.tvremote.ads.AdManager
import com.js.tvremote.net.TvDevice
import com.js.tvremote.ui.AboutScreen
import com.js.tvremote.ui.AppsScreen
import com.js.tvremote.ui.DiscoverScreen
import com.js.tvremote.ui.RemoteScreen
import com.js.tvremote.ui.theme.JsTvRemoteTheme

private sealed class Screen {
    data object Discover : Screen()
    data class Remote(val device: TvDevice) : Screen()
    data class AppsList(val device: TvDevice) : Screen()
    data object About : Screen()
    data object Privacy : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        AdManager.init(this)

        setContent {
            JsTvRemoteTheme {
                var commandCount by remember { mutableIntStateOf(0) }
                var screen by remember { mutableStateOf<Screen>(Screen.Discover) }

                when (val current = screen) {
                    Screen.Discover -> DiscoverScreen(
                        onDeviceSelected = { device -> screen = Screen.Remote(device) },
                        onAboutClick = { screen = Screen.About }
                    )
                    is Screen.Remote -> RemoteScreen(
                        device = current.device,
                        onBack = { screen = Screen.Discover },
                        onCommandSent = {
                            commandCount++
                            if (commandCount % 12 == 0) {
                                AdManager.maybeShowInterstitial(this@MainActivity)
                            }
                        },
                        onOpenApps = { screen = Screen.AppsList(current.device) },
                        onOpenAjustes = { screen = Screen.About }
                    )
                    is Screen.AppsList -> AppsScreen(
                        device = current.device,
                        onSelectRemoto = { screen = Screen.Remote(current.device) },
                        onSelectAjustes = { screen = Screen.About }
                    )
                    Screen.About -> AboutScreen(
                        onBack = { screen = Screen.Discover },
                        onPrivacyClick = { screen = Screen.Privacy },
                        onAdPrivacyClick = { AdManager.showPrivacyOptions(this@MainActivity) }
                    )
                    Screen.Privacy -> com.js.tvremote.ui.PrivacyPolicyScreen(onBack = { screen = Screen.About })
                }
            }
        }
    }
}
