package com.github.sdpcoachme.errorhandling

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.auth.LoginActivity
import com.github.sdpcoachme.ui.theme.CoachMeTheme

/**
 * Activity that handles errors related to intent extras.
 *
 * It displays the error message received or a generic
 * error message if none was received. Then the user can
 * go back to the login activity.
 */
class IntentExtrasErrorHandlerActivity : ComponentActivity() {
    class TestTags {
        class TextFields {
            companion object {
                const val ERROR_MESSAGE_FIELD = "errorMessage"
            }
        }
        class Buttons {
            companion object {
                const val GO_TO_LOGIN_BUTTON = "goToLoginButton"
            }
        }
    }

    private val GENERIC_ERROR_MESSAGE = "An error occurred. Please return to the login page and retry."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO handle the null better here
        val errorMsg = intent.getStringExtra("errorMsg") ?: GENERIC_ERROR_MESSAGE

        setContent {
            CoachMeTheme {
                Surface(
                    color = MaterialTheme.colors.background
                ) {
                    IntentExtrasErrorScreen(errorMsg)
                }
            }
        }
    }

    @Composable
    fun IntentExtrasErrorScreen(errorMsg: String) {
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = errorMsg,
                Modifier.testTag(TestTags.TextFields.ERROR_MESSAGE_FIELD))
            Spacer(modifier = Modifier.height(16.dp))
            Button(modifier = Modifier.testTag(TestTags.Buttons.GO_TO_LOGIN_BUTTON),
                onClick = {
                    val intent = Intent(context, LoginActivity::class.java)
                    startActivity(intent)
                }) {
                    Text(text = "RETURN TO LOGIN PAGE")
                }
        }
    }
}