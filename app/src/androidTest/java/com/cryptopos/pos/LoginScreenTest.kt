package com.cryptopos.pos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cryptopos.pos.core.theme.CryptoPosTheme
import com.cryptopos.pos.features.auth.LoginScreenContent
import org.junit.Rule
import org.junit.Test

/**
 * Instrument smoke: real login form content (no Hilt), matching production copy.
 */
class LoginScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loginForm_isDisplayed() {
        composeRule.setContent {
            CryptoPosTheme {
                LoginScreenContent(
                    email = "",
                    password = "",
                    loading = false,
                    error = null,
                    onEmailChange = {},
                    onPasswordChange = {},
                    onLogin = {},
                )
            }
        }
        composeRule.onNodeWithText("Welcome back").assertIsDisplayed()
        composeRule.onNodeWithText("Email").assertIsDisplayed()
        composeRule.onNodeWithText("Sign in").assertIsDisplayed()
    }
}
