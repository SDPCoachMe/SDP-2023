package com.github.sdpcoachme.firebase.auth;

import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;

public interface GoogleAuthenticator {

    void signIn(ActivityResultLauncher<Intent> signInLauncher);

    void delete(Context context, TextView textView);

    void signOut(Context context, TextView textView);
}
