package com.github.sdpcoachme.rating

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import java.util.concurrent.CompletableFuture

class RatingActivity: ComponentActivity() {

    class TestTags {
        companion object {

            const val TITLE = "title"

            class Buttons {
                companion object {
                    const val DONE = "doneButton"
                    const val CANCEL = "cancelButton"
                }
            }
        }
    }

    companion object {

        private const val INITIAL_KEY = "initialValue"
        private const val FINAL_KEY = "finalValue"
        private const val COACH_NAME = "coachName"

        private const val DEFAULT_COACH_NAME = "the coach"

        fun getIntent(
            context: Context,
            coachName: String? = null,
            initialValue: Int = 0
        ): Intent {
            val intent = Intent(context, RatingActivity::class.java)
            intent.putExtra(COACH_NAME, coachName)
            intent.putExtra(INITIAL_KEY, initialValue)
            return intent
        }

        fun getHandler(caller: ActivityResultCaller): (Intent) -> CompletableFuture<Int> {
            // Keep a reference to the future so we can complete it later
            lateinit var futureValue: CompletableFuture<Int>
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
                        futureValue.completeExceptionally(RatingCancelledException())
                    }
                    else -> {
                        // There was an unknown error
                        futureValue.completeExceptionally(RatingFailedException())
                    }
                }
            }

            return {
                    intent ->
                futureValue = CompletableFuture<Int>()
                launcher.launch(intent)
                futureValue
            }
        }

        private fun getValueFromIntent(intent: Intent): Int {
            return intent.getIntExtra(FINAL_KEY, 3)
        }

        // Used to handle rating activity errors or cancelling
        class RatingFailedException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
            constructor(cause: Throwable) : this(null, cause)
        }
        class RatingCancelledException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
            constructor(cause: Throwable) : this(null, cause)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialRating = intent.getIntExtra(INITIAL_KEY, 0)
        val title = "Rate " + (intent.getStringExtra(COACH_NAME) ?: DEFAULT_COACH_NAME)

        setContent {
            CoachMeTheme {
                Surface(color = MaterialTheme.colors.background) {
                    RatingBar(
                        title = title,
                        initialRating = initialRating,
                        onSubmit = {
                            setResult(RESULT_OK, Intent().putExtra(FINAL_KEY, it))
                            finish()
                        },
                        onCancel = {
                            setResult(RESULT_CANCELED)
                            finish()
                        }
                    )
                }
            }
        }
    }
}