package com.cryptopos.pos

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cryptopos.pos.core.theme.CryptoPosTheme
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loginForm_isDisplayed() {
        composeRule.setContent {
            CryptoPosTheme {
                Column {
                    Text("Welcome back")
                    Text("Sign in to accept cards and settle to USDT")
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        label = { Text("Email") },
                    )
                }
            }
        }
        composeRule.onNodeWithText("Welcome back").assertIsDisplayed()
        composeRule.onNodeWithText("Email").assertIsDisplayed()
    }
}
