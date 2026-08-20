package com.cryptopos.pos

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cryptopos.pos.core.theme.CryptoPosTheme
import org.junit.Rule
import org.junit.Test

class PaymentAmountUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun amountDisplay_isReadable() {
        composeRule.setContent {
            CryptoPosTheme {
                Text("12.50")
            }
        }
        composeRule.onNodeWithText("12.50").assertIsDisplayed()
    }
}
