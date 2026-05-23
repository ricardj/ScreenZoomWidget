package com.gemini.zoomwidget

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gemini.zoomwidget.ui.theme.ScreenZoomWidgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScreenZoomWidgetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InstructionScreen()
                }
            }
        }
    }
}

private fun isPermissionGranted(context: Context): Boolean {
    return context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun InstructionScreen() {
    val context = LocalContext.current
    val permissionGranted = isPermissionGranted(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (permissionGranted) {
            PermissionGrantedView()
        } else {
            PermissionDeniedView()
        }
    }
}

@Composable
fun PermissionGrantedView() {
    Text(
        text = "Permission Granted!",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    Text(
        text = "You can now add the widget to your home screen.",
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
fun PermissionDeniedView() {
    val adbCommand = "adb shell pm grant com.gemini.zoomwidget android.permission.WRITE_SECURE_SETTINGS"

    Text(
        text = "Setup Required",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    Text(
        text = "To use the widget, you need to grant a special permission using Android Debug Bridge (adb). This is a one-time step.",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    Text(
        text = "1. Enable USB Debugging on your phone (in Developer Options).",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Text(
        text = "2. Connect your phone to a computer with ADB installed.",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Text(
        text = "3. Run the following command in your terminal:",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray)
            .padding(16.dp)
    ) {
        Text(
            text = adbCommand,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = Color.Black
        )
    }
    Text(
        text = "
After running the command, restart the app.",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 16.dp)
    )
}
