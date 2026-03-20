package com.campuscast.tvplayer.feature.activation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.campuscast.tvplayer.core.i18n.I18n

@Composable
fun ActivationScreen(
    locale: String,
    deviceId: String?,
    code: String?,
    expiresSeconds: Int,
    phase: ActivationPhase,
    error: String?,
    onRetry: () -> Unit,
) {
    val resolvedLocale = I18n.normalizeLocale(locale)
    val t = { key: String -> I18n.t(resolvedLocale, key) }
    val minutes = expiresSeconds / 60
    val seconds = expiresSeconds % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 30.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(t("playback.idleSubtitle"), style = MaterialTheme.typography.headlineMedium)
                Text(
                    t("activation.title"),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )

                when (phase) {
                    ActivationPhase.LOADING -> {
                        CircularProgressIndicator(modifier = Modifier.padding(top = 28.dp))
                        Text(
                            t("activation.requesting"),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }

                    ActivationPhase.POLLING -> {
                        Text(
                            t("activation.enterCode"),
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
                                text =
                                    "${t("activation.expiresIn")} ${
                                        "%d:%02d".format(minutes, seconds)
                                    }",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = t("activation.waiting"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }

                    ActivationPhase.ACTIVATED -> {
                        Text(
                            t("activation.activated"),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(top = 28.dp),
                        )
                        CircularProgressIndicator(modifier = Modifier.padding(top = 18.dp))
                        Text(
                            t("activation.preparingPlayback"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }

                    ActivationPhase.ERROR -> {
                        Text(
                            error ?: t("activation.failed"),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                        Button(onClick = onRetry, modifier = Modifier.padding(top = 18.dp)) {
                            Text(t("activation.requestNew"))
                        }
                    }
                }

                Text(
                    text = "${t("activation.deviceId")}: ${deviceId ?: t("activation.notSet")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 36.dp),
                )
            }
        }
    }
}
