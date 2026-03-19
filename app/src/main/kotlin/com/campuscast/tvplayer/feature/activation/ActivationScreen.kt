package com.campuscast.tvplayer.feature.activation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campuscast.tvplayer.app.ActivationPhase

@Composable
fun ActivationScreen(
    deviceId: String?,
    code: String?,
    expiresSeconds: Int,
    phase: ActivationPhase,
    error: String?,
    onRetry: () -> Unit,
) {
    val minutes = expiresSeconds / 60
    val seconds = expiresSeconds % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 54.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("CampusCast Player", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Activation",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 8.dp),
        )

        when (phase) {
            ActivationPhase.LOADING -> {
                Text(
                    "Requesting activation code...",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }

            ActivationPhase.POLLING -> {
                Text(
                    "Enter this code in CMS",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp),
                )
                Text(
                    text = code ?: "------",
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(18.dp),
                        )
                        .padding(horizontal = 34.dp, vertical = 24.dp),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 56.sp,
                        letterSpacing = 5.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )

                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Expires in %d:%02d".format(minutes, seconds),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Waiting for activation...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            ActivationPhase.ACTIVATED -> {
                Text(
                    "Player activated",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 28.dp),
                )
            }

            ActivationPhase.ERROR -> {
                Text(
                    error ?: "Activation failed",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 24.dp),
                )
                Button(onClick = onRetry, modifier = Modifier.padding(top = 18.dp)) {
                    Text("Request new code")
                }
            }
        }

        Text(
            text = "Device ID: ${deviceId ?: "Not set"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 36.dp),
        )
    }
}
