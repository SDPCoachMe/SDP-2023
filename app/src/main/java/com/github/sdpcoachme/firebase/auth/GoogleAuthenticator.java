package com.github.sdpcoachme.firebase.auth;

import android.content.Context;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;

import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;

import java.util.function.Consumer;

/**
 * Interface responsible for the Google sign in process
 */
public interface GoogleAuthenticator {

    /**
     * Creates a sign in intent and launches it using the given launcher
     *
     * @param signInLauncher the launcher to use
     */
    void signIn(ActivityResultLauncher<Intent> signInLauncher);

    /**
     * Deletes the current user
     *
     * @param context the context to use
     * @param onComplete the callback to call on completion
     */
    void delete(Context context, Runnable onComplete);

    /**
     * Signs out the current user
     *
     * @param context the context to use
     * @param onComplete the callback to call on completion
     */
    void signOut(Context context, Runnable onComplete);

    /**
     * Handles the result of the sign in intent and calls the appropriate callback based on success or failure
     *
     * @param result the result of the sign in intent
     * @param onSuccess the callback to call on success
     * @param onFailure the callback to call on failure
     */
    void onSignInResult(FirebaseAuthUIAuthenticationResult result, Consumer<String> onSuccess, Runnable onFailure);
}
