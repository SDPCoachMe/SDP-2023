package com.github.sdpcoachme.firebase.auth;

import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;

import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;

public interface GoogleAuthenticator {

    void signIn(ActivityResultLauncher<Intent> signInLauncher);

    void delete(Context context, TextView textView);

    void signOut(Context context, TextView textView);

    void onSignInResult(FirebaseAuthUIAuthenticationResult result, TextView textView);
}
