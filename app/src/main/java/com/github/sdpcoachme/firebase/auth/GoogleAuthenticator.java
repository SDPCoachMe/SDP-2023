package com.github.sdpcoachme.firebase.auth;

import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;

import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;

import java.util.function.Consumer;
import java.util.function.Function;

public interface GoogleAuthenticator {

    void signIn(ActivityResultLauncher<Intent> signInLauncher);

    void delete(Context context, Runnable onComplete);

    void signOut(Context context, Runnable onComplete);

    void onSignInResult(FirebaseAuthUIAuthenticationResult result, Consumer<String> onSuccess, Runnable onFailure);
}
