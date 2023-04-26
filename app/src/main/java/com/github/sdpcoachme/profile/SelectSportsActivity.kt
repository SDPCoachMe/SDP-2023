package com.github.sdpcoachme.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.profile.SelectSportsActivity.TestTags.Buttons.Companion.CANCEL
import com.github.sdpcoachme.profile.SelectSportsActivity.TestTags.Buttons.Companion.DONE
import com.github.sdpcoachme.profile.SelectSportsActivity.TestTags.Companion.TITLE
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import java.util.concurrent.CompletableFuture

/**
 * Activity that allows the user to select sports. To launch this activity from another caller activity,
 * first run getHandler from the onCreate method of your caller activity. This registers the handler
 * with the caller activity. Then, use the handler (lambda returned by getHandler) to launch the activity.
 *
 * The handler can be called from anywhere in the caller activity, and takes an Intent as a parameter.
 * The Intent should be created using the getIntent method of this class. The handler returns a
 * CompletableFuture that completes with the result value inputted by the user.
 *
 * See https://developer.android.com/training/basics/intents/result for more information.
 */
class SelectSportsActivity : ComponentActivity() {

    class TestTags {
        class ListRowTag(sport: Sports) {
            val TEXT = "${sport.sportName}Text"
            val ICON = "${sport.sportName}Icon"
            val ROW = "${sport.sportName}Row"
        }
        class MultiSelectListTag {
            companion object {
                const val LAZY_SELECT_COLUMN = "lazySelectColumn"
                val ROW_TEXT_LIST = Sports.values().map { ListRowTag(it) }
            }
        }
        class Buttons {
            companion object {
                const val DONE = "done"
                const val CANCEL = "cancel"
            }
        }
        companion object {
            const val TITLE = "title"
            const val COLUMN = "column"
        }
    }

    // TODO: a lot of code duplication between this class and EditTextActivity.
    //  Consider refactoring to a common base class, when time allows.
    companion object {

        private const val INITIAL_VALUE_KEY = "initialValue"
        private const val TITLE_KEY = "title"

        const val DEFAULT_TITLE = "Select sports"

        private const val RETURN_VALUE_KEY = "returnValue"

        /**
         * Creates an Intent that can be used to launch this activity.
         *
         * @param context The context of the caller activity.
         * @param initialValue The initial selected sports to display.
         * @param title The title displayed in the app bar.
         * @return An Intent that can be used to launch this activity.
         */
        fun getIntent(
            context: Context,
            title: String? = null,
            initialValue: List<Sports>? = null
        ): Intent {
            val intent = Intent(context, SelectSportsActivity::class.java)
            intent.putExtra(TITLE_KEY, title)
            intent.putExtra(INITIAL_VALUE_KEY, initialValue?.toTypedArray())
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
        fun getHandler(caller: ActivityResultCaller): (Intent) -> CompletableFuture<List<Sports>> {
            // Keep a reference to the future so we can complete it later
            lateinit var futureValue: CompletableFuture<List<Sports>>
            // Set up lambda that handles result
            val launcher = caller.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result: ActivityResult ->
                when (result.resultCode) {
                    RESULT_OK -> {
                        result.data!!.let {
                            futureValue.complete(getValueFromIntent(it))
                        }
                    }
                    RESULT_CANCELED -> {
                        // The user canceled the operation
                        futureValue.completeExceptionally(SelectSportsCancelledException())
                    }
                    else -> {
                        // There was an unknown error
                        futureValue.completeExceptionally(SelectSportsFailedException())
                    }
                }
            }

            return { intent ->
                futureValue = CompletableFuture<List<Sports>>()
                launcher.launch(intent)
                futureValue
            }
        }

        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        private fun getValueFromIntent(intent: Intent): List<Sports> {
            // Note: here we use this deprecated method, since using the new method requires API
            // TIRAMISU, and we don't require it for our app.
            // Note 2: We never send null anyways, so double bang is safe here.
            return (intent.getSerializableExtra(RETURN_VALUE_KEY)!! as Array<Sports>).toList()
        }

        // Used to handle edit text activity errors or cancelling
        class SelectSportsFailedException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
            constructor(cause: Throwable) : this(null, cause)
        }
        class SelectSportsCancelledException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
            constructor(cause: Throwable) : this(null, cause)
        }
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Note: here we use this deprecated method, since using the new method requires API
        // TIRAMISU, and we don't require it for our app.
        val initialValue = (intent.getSerializableExtra(INITIAL_VALUE_KEY) as? Array<Sports>)?.toList() ?: emptyList()
        val title = intent.getStringExtra(TITLE_KEY) ?: DEFAULT_TITLE

        setContent {
            CoachMeTheme {
                SelectSportsLayout(
                    onSubmit = {
                        setResult(RESULT_OK, Intent().putExtra(RETURN_VALUE_KEY, it.toTypedArray()))
                        finish()
                    },
                    onCancel = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    initialValue = initialValue,
                    title = title
                )
            }
        }
    }

    /**
     * The layout of the activity.
     *
     * @param initialValue The initial selected sports to display.
     * @param onCancel A callback to be called when the user cancels the operation.
     * @param onSubmit A callback to be called when the user submits the operation.
     * @param title The title displayed in the app bar.
     */
    @Composable
    fun SelectSportsLayout(
        initialValue: List<Sports>,
        onCancel: () -> Unit,
        onSubmit: (List<Sports>) -> Unit,
        title: String
    ) {
        var sportItems by remember {
            mutableStateOf(Sports.values().map { ListItem(it, initialValue.contains(it)) })
        }

        val toggleSelectSport: (Sports) -> Unit = { sport ->
            sportItems = sportItems.map { item ->
                if (item.element == sport) {
                    item.copy(selected = !item.selected)
                } else {
                    item
                }
            }
        }

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
                                onSubmit(sportItems.filter { it.selected }.map { it.element })
                            },
                            modifier = Modifier.testTag(DONE)
                        ) {
                            Icon(Icons.Filled.Done, "Done",
                                tint = MaterialTheme.colors.onPrimary)
                        }
                    })
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .testTag(TestTags.COLUMN),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MultiSelectList(
                    items = sportItems,
                    toggleSelectSport = toggleSelectSport
                )
            }
        }
    }

    /**
     * Composable that displays a list of sports that can be selected.
     * @param items The list of sports to display.
     * @param toggleSelectSport A function that will be called whenever a sport is selected or
     * unselected.
     */
    @Composable
    fun MultiSelectList(items: List<ListItem<Sports>>, toggleSelectSport: (Sports) -> Unit) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.MultiSelectListTag.LAZY_SELECT_COLUMN)
        ) {
            items(items.size) { i ->
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            toggleSelectSport(Sports.values()[i])
                        }
                        .padding(16.dp)
                        .testTag(TestTags.MultiSelectListTag.ROW_TEXT_LIST[i].ROW),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = items[i].element.sportName,
                        modifier = Modifier.testTag(
                            TestTags.MultiSelectListTag.ROW_TEXT_LIST[i].TEXT))
                    Icon(
                        imageVector = if (items[i].selected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colors.primary,
                        modifier = if (items[i].selected) Modifier.size(20.dp).testTag(TestTags.MultiSelectListTag.ROW_TEXT_LIST[i].ICON)
                                    else Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    /**
     * A data class for a list item
     */
    data class ListItem<A>(
        val element: A,
        val selected: Boolean
    )
}