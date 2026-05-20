package com.stenograph.app.ui.dictation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stenograph.app.ui.theme.*

@Composable
fun DictationScreen(
    viewModel: DictationViewModel = viewModel(),
    onRePair: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val connected by viewModel.connected.collectAsStateWithLifecycle()
    val isDictating by viewModel.isDictating.collectAsStateWithLifecycle()
    val previewText by viewModel.previewText.collectAsStateWithLifecycle()
    val listening by viewModel.listening.collectAsStateWithLifecycle()
    val authRejected by viewModel.authRejected.collectAsStateWithLifecycle()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            viewModel.toggleDictation()
        }
    }

    // Reconnect on app resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Keep screen on while dictating
    val activity = context as? android.app.Activity
    DisposableEffect(isDictating) {
        if (isDictating) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Pulsing animation for active mic button
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Re-pair button — top start
            if (onRePair != null) {
                TextButton(
                    onClick = {
                        viewModel.clearPairing()
                        onRePair()
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                ) {
                    Text("Re-pair", fontSize = 12.sp)
                }
            }

            // Connection status indicator — top end
            ConnectionIndicator(
                connected = connected,
                authRejected = authRejected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            )

            // Main content centered
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Preview text area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .background(
                            color = PreviewBackground.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium,
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        previewText.isNotBlank() -> {
                            Text(
                                text = previewText,
                                color = PreviewText,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                        isDictating -> {
                            Text(
                                text = "Listening...",
                                color = PreviewText.copy(alpha = 0.5f),
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                        authRejected -> {
                            Text(
                                text = "Token expired — tap Re-pair to reconnect",
                                color = Color(0xFFFF6B6B),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                        else -> {
                            Text(
                                text = if (connected) "Tap the mic to start" else "Waiting for PC connection...",
                                color = PreviewText.copy(alpha = 0.35f),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Mic button
                val micScale = if (isDictating) pulseScale else 1f
                val micColor = when {
                    !connected -> Color.Gray
                    isDictating -> MicActiveRed
                    else -> MicIdleBlue
                }

                Button(
                    onClick = {
                        vibrateShort(context)
                        if (!hasAudioPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.toggleDictation()
                        }
                    },
                    modifier = Modifier
                        .size(120.dp)
                        .scale(micScale),
                    shape = CircleShape,
                    enabled = connected,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = micColor,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                    ),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Icon(
                        imageVector = if (isDictating) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (isDictating) "Stop dictation" else "Start dictation",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Undo button
                if (isDictating || previewText.isNotBlank()) {
                    TextButton(
                        onClick = { viewModel.undo() },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Undo")
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

        }
    }
}

@Composable
private fun ConnectionIndicator(
    connected: Boolean,
    authRejected: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (color, label) = when {
            authRejected -> Color(0xFFFF6B6B) to "Re-pair needed"
            connected -> ConnectedGreen to "Connected"
            else -> DisconnectedGray to "Disconnected"
        }
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = color, shape = CircleShape),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = color, fontSize = 12.sp)
    }
}

private fun vibrateShort(context: android.content.Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator?.vibrate(
            VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
