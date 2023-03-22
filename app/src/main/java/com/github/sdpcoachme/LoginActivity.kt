package com.github.sdpcoachme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.github.sdpcoachme.firebase.auth.Authenticator
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.firebase.database.NoSuchKeyException
import com.github.sdpcoachme.ui.theme.CoachMeTheme


class LoginActivity : ComponentActivity() {

    class TestTags {
        companion object {
            const val INFO_TEXT = "signInInfo"
        }
        class Buttons {
            companion object {
                const val SIGN_IN = "signInButton"
                const val SIGN_OUT = "signOutButton"
                const val DELETE_ACCOUNT = "deleteAccountButton"
            }
        }
    }

    private lateinit var database : Database
    private var signInInfo: String by mutableStateOf("Not signed in")
    private lateinit var authenticator: Authenticator
    private val signInLauncher: ActivityResultLauncher<Intent> = registerForActivityResult (
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        authenticator.onSignInResult(
            res,
            { email -> launchPostLoginActivity(email ?: "") },
            { errorMsg -> signInInfo = errorMsg.toString() }
        )
    }

    // Allows to use testTagsAsResourceId feature (since tests are done with UI Automator, it is
    // necessary to map test tags to resource IDs)
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authenticator = (application as CoachMeApplication).authenticator
        database =  (application as CoachMeApplication).database

        setContent {
            CoachMeTheme {
                Scaffold(
                    // Enables for all composables in the hierarchy.
                    modifier = Modifier.semantics {
                        testTagsAsResourceId = true
                    }
                ) {
                    // Need to pass padding to child node
                    innerPadding ->
                        AuthenticationForm(
                            signInInfo = this.signInInfo,
                            context = this,
                            modifier = Modifier.padding(innerPadding)
                        )
                }
            }
        }
    }

    private fun launchPostLoginActivity(email : String) {
        signInInfo = email
        val query = database.getUser(email)
        query.handle { result, exception ->
            when (exception) {
                null -> {
                    // User is already in the database
                    // TODO the database instance should cast to the correct type, not the caller. Moreover, with the current implementation it is not possible to cast properly.
                    //val user = result as Map<*, *>
                    val intent = Intent(this, DashboardActivity::class.java)
                    // TODO send user object instead of individual field. This requires that UserInfo is serializable
                    // TODO later on, have a dedicated store or cache for all the database data
                    intent.putExtra("email", result.email)
                    startActivity(intent)
                }
                is NoSuchKeyException -> {
                    // User is not in the database
                    val intent = Intent(this, SignupActivity::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                }
                else -> {
                    // TODO handle this error better
                    signInInfo = "Unknown error"
                }
            }
        }
    }

    /**
     * Deletes the google account from the device
     */
    fun deleteAccount() {
        authenticator.delete(this) { signInInfo = getString(R.string.account_deleted) }
    }

    /**
     * Signs into the google account
     */
    fun signIntoAccount() {
        authenticator.signIn(signInLauncher)
    }

    /**
     * Signs out of the google account
     */
    fun signOutOfAccount() {
        authenticator.signOut(this) { signInInfo = getString(R.string.signed_out) }
    }
}

@Composable
fun AuthenticationForm(signInInfo: String, context: LoginActivity, modifier: Modifier = Modifier) {

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            modifier = Modifier.testTag(LoginActivity.TestTags.INFO_TEXT),
            text = signInInfo,
        )

        Button(
            modifier = Modifier.testTag(LoginActivity.TestTags.Buttons.SIGN_IN),
            onClick = {
                context.signIntoAccount()
            })
        { Text(stringResource(id = R.string.sign_in_button_text)) }
        Button(
            modifier = Modifier.testTag(LoginActivity.TestTags.Buttons.SIGN_OUT),
            onClick = {
                context.signOutOfAccount()
            })
        { Text(stringResource(id = R.string.sign_out_button_text)) }
        Button(
            modifier = Modifier.testTag(LoginActivity.TestTags.Buttons.DELETE_ACCOUNT),
            onClick = {
                context.deleteAccount()
            })
        { Text(stringResource(id = R.string.delete_account_button_text)) }
    }
}