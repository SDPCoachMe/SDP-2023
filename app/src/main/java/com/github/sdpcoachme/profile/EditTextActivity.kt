package com.github.sdpcoachme.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.profile.EditTextActivity.TestTags.Companion.Buttons.Companion.CANCEL
import com.github.sdpcoachme.profile.EditTextActivity.TestTags.Companion.Buttons.Companion.DONE
import com.github.sdpcoachme.profile.EditTextActivity.TestTags.Companion.TITLE
import com.github.sdpcoachme.profile.EditTextActivity.TestTags.Companion.TextFields.Companion.MAIN
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import java.util.concurrent.CompletableFuture

/**
 * Activity that allows the user to enter text. To launch this activity from another caller activity,
 * first run getHandler from the onCreate method of your caller activity. This registers the handler
 * with the caller activity. Then, use the handler (lambda returned by getHandler) to launch the activity.
 *
 * The handler can be called from anywhere in the caller activity, and takes an Intent as a parameter.
 * The Intent should be created using the getIntent method of this class, and specifies the initial
 * value, label, placeholder, and title of the text field. The handler returns a CompletableFuture
 * that completes with the result value inputted by the user.
 *
 * See https://developer.android.com/training/basics/intents/result for more information.
 */
class EditTextActivity : ComponentActivity() {

    class TestTags {
        companion object {

            const val TITLE = "title"

            class TextFields {
                companion object {
                    const val MAIN = "textField"
                }
            }
            class Buttons {
                companion object {
                    const val DONE = "doneButton"
                    const val CANCEL = "cancelButton"
                }
            }
        }
    }

    companion object {

        private const val INITIAL_VALUE_KEY = "initialValue"
        private const val LABEL_KEY = "label"
        private const val PLACEHOLDER_KEY = "placeholder"
        private const val TITLE_KEY = "title"

        const val DEFAULT_PLACEHOLDER = "Enter text"
        const val DEFAULT_TITLE = "Edit"

        private const val RETURN_VALUE_KEY = "returnValue"

        /**
         * Creates an Intent that can be used to launch this activity.
         *
         * @param context The context of the caller activity.
         * @param initialValue The initial value of the text field.
         * @param label The label of the text field.
         * @param placeholder The placeholder of the text field.
         * @param title The title displayed in the app bar.
         * @return An Intent that can be used to launch this activity.
         */
        fun getIntent(
            context: Context,
            title: String? = null,
            placeholder: String? = null,
            label: String? = null,
            initialValue: String? = null
        ): Intent {
            val intent = Intent(context, EditTextActivity::class.java)
            intent.putExtra(TITLE_KEY, title)
            intent.putExtra(PLACEHOLDER_KEY, placeholder)
            intent.putExtra(LABEL_KEY, label)
            intent.putExtra(INITIAL_VALUE_KEY, initialValue)
            return intent
        }

        /**
         * Creates a handler that can be used to launch this activity. This method should be called
         * from the onCreate method of the caller activity. The returned handler can be called from
         * anywhere in the caller activity, and takes an Intent as a parameter. The Intent should be
         * created using the getIntent method of this class.
         *
         * @param caller The caller activity.
         * @return A handler that can be used to launch this activity.
         */
        fun getHandler(caller: ActivityResultCaller): (Intent) -> CompletableFuture<String> {
            // Keep a reference to the future so we can complete it later
            lateinit var futureValue: CompletableFuture<String>
            // Set up lambda that handles result
            val launcher = caller.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                result: ActivityResult ->
                    when (result.resultCode) {
                        RESULT_OK -> {
                            result.data!!.let {
                                futureValue.complete(getValueFromIntent(it))
                            }
                        }
                        RESULT_CANCELED -> {
                            // The user canceled the operation
                            futureValue.completeExceptionally(EditTextCancelledException())
                        }
                        else -> {
                            // There was an unknown error
                            futureValue.completeExceptionally(EditTextFailedException())
                        }
                    }
            }

            return {
                intent ->
                    futureValue = CompletableFuture<String>()
                    launcher.launch(intent)
                    futureValue
            }
        }

        private fun getValueFromIntent(intent: Intent): String {
            // We never send null anyways
            return intent.getStringExtra(RETURN_VALUE_KEY)!!
        }

        // Used to handle edit text activity errors or cancelling
        class EditTextFailedException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
            constructor(cause: Throwable) : this(null, cause)
        }
        class EditTextCancelledException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
            constructor(cause: Throwable) : this(null, cause)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialValue = intent.getStringExtra(INITIAL_VALUE_KEY) ?: ""
        val title = intent.getStringExtra(TITLE_KEY) ?: DEFAULT_TITLE
        val label: String? = intent.getStringExtra(LABEL_KEY)
        var placeholder: String? = intent.getStringExtra(PLACEHOLDER_KEY)
        if (label == null && placeholder == null) {
            placeholder = DEFAULT_PLACEHOLDER
        }

        setContent {
            CoachMeTheme {
                EditTextLayout(
                    onSubmit = {
                        setResult(RESULT_OK, Intent().putExtra(RETURN_VALUE_KEY, it))
                        finish()
                    },
                    onCancel = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    initialValue = initialValue,
                    title = title,
                    label = label,
                    placeholder = placeholder
                )
            }
        }
    }
}

/**
 * The layout for the EditTextActivity.
 *
 * @param initialValue The initial value of the text field.
 * @param onCancel The callback to be called when the user cancels the operation.
 * @param onSubmit The callback to be called when the user submits the operation.
 * @param title The title displayed in the app bar.
 * @param label The label of the text field.
 * @param placeholder The placeholder of the text field.
 */
@Composable
fun EditTextLayout(
    initialValue: String,
    onCancel: () -> Unit,
    onSubmit: (String) -> Unit,
    title: String,
    label: String?,
    placeholder: String?
) {
    var value by remember { mutableStateOf(initialValue) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(title, modifier = Modifier.testTag(TITLE))
                },
                navigationIcon = {
                    IconButton(onClick = onCancel, modifier = Modifier.testTag(CANCEL)) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onSubmit(value)
                        },
                        modifier = Modifier.testTag(DONE)
                    ) {
                        Icon(Icons.Filled.Done, "Done",
                            tint = MaterialTheme.colors.onPrimary)
                    }
                })
        }
    ) {
        padding ->
            Column(
                modifier = Modifier.padding(padding)
            ) {
                TextField(
                    modifier = Modifier
                        .testTag(MAIN)
                        .padding(20.dp)
                        .fillMaxWidth(),
                    value = value,
                    onValueChange = { value = it },
                    label = label?.let { { Text(it) } },
                    placeholder = placeholder?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onSubmit(value)
                        }
                    )
                )
            }
    }
}