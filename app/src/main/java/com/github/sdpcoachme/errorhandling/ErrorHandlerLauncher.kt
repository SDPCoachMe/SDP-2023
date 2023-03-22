package com.github.sdpcoachme.errorhandling

import android.content.Context
import android.content.Intent

/**
 * This class is used to launch the error handlers from anywhere in the app.
 */
class ErrorHandlerLauncher {

    /**
     * Launches the IntentExtrasErrorActivity with the error message passed as an argument.
     *
     * @param context The context of the activity that is launching the IntentExtrasErrorActivity.
     * @param errorMsg The error message to be displayed in the IntentExtrasErrorActivity.
     */
    fun launchExtrasErrorHandler(context: Context, errorMsg: String) {
        val intent = Intent(context , IntentExtrasErrorHandlerActivity::class.java)
        intent.putExtra("errorMsg", errorMsg)
        context.startActivity(intent)
    }
}