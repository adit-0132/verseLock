package com.verselock.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verselock.ui.theme.DmSansFamily
import com.verselock.ui.theme.SpaceGroteskFamily
import com.verselock.util.dataStore
import com.verselock.util.PrefKeys
import com.verselock.util.setLockScreenEnabled
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsScreen()
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val lockScreenEnabled by context.dataStore.data
        .map { it[PrefKeys.LOCK_SCREEN_ENABLED] ?: true }
        .collectAsStateWithLifecycle(initialValue = true)

    val notificationAccessGranted = remember {
        Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )?.contains(context.packageName) == true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080810))
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "verseLock",
            fontSize = 32.sp,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            "Lyrics on your lock screen",
            fontSize = 14.sp,
            fontFamily = DmSansFamily,
            color = Color.White.copy(alpha = 0.45f)
        )

        Spacer(Modifier.height(8.dp))

        // Permission warning
        if (!notificationAccessGranted) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x20ef4444),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Notification Access Required",
                        fontSize = 13.sp,
                        fontFamily = DmSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFef4444)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "verseLock needs notification access to read what's playing. Tap to open settings.",
                        fontSize = 12.sp,
                        fontFamily = DmSansFamily,
                        color = Color.White.copy(alpha = 0.60f),
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFef4444))
                    ) {
                        Text("Open Settings", fontSize = 12.sp)
                    }
                }
            }
        }

        // Main toggle
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0e0e18),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Lock Screen Lyrics",
                        fontSize = 14.sp,
                        fontFamily = DmSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        "Force screen on and show lyrics while playing",
                        fontSize = 12.sp,
                        fontFamily = DmSansFamily,
                        color = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.widthIn(max = 220.dp),
                        lineHeight = 18.sp
                    )
                }
                Switch(
                    checked = lockScreenEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { context.setLockScreenEnabled(enabled) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF7C3AED)
                    )
                )
            }
        }
    }
}
