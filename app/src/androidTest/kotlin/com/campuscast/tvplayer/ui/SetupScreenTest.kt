package com.campuscast.tvplayer.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.campuscast.tvplayer.core.model.AppConfig
import com.campuscast.tvplayer.feature.setup.SetupScreen
import com.campuscast.tvplayer.ui.theme.CampusCastTheme
import org.junit.Rule
import org.junit.Test

class SetupScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun setupScreenRendersPrimaryAction() {
        composeRule.setContent {
            CampusCastTheme {
                SetupScreen(
                    config = AppConfig(),
                    locale = "en",
                    error = null,
                    onSubmit = { _, _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("Continue to Activation").assertIsDisplayed()
    }
}
