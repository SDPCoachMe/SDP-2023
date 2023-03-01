package com.github.sdpcoachme.firebase.auth;

import static android.app.Activity.RESULT_OK;

import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.github.sdpcoachme.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class RealGoogleAuthenticator implements GoogleAuthenticator {

    @Inject
    public RealGoogleAuthenticator() {
        // Potential constructor logic comes here
    }

    public void createSignInIntent(ActivityResultLauncher<Intent> signInLauncher) {
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Collections.singletonList(
                new AuthUI.IdpConfig.GoogleBuilder().build());

        // Create and launch sign-in intent
        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build();

        signInLauncher.launch(signInIntent);
    }

    @Override
    public void onSignInResult(FirebaseAuthUIAuthenticationResult result, TextView textView) {
        IdpResponse response = result.getIdpResponse();
        if (result.getResultCode() == RESULT_OK) {
            // Successfully signed in
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            // ...
            assert user != null;
            String signInText = "Signed in as: " + user.getEmail();
            textView.setText(signInText);

        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
            if (response == null || response.getError() == null) {
                textView.setText(R.string.sign_in_failed);
            } else {
                 String error = "Sign in failed: " + response.getError().getErrorCode();
                textView.setText(error);
            }
        }
    }

    @Override
    public void signIn(ActivityResultLauncher<Intent> signInLauncher) {
        createSignInIntent(signInLauncher);
    }

    @Override
    public void delete(Context context, TextView textView) {
        AuthUI.getInstance()
                .delete(context)
                .addOnCompleteListener(task ->
                        textView.setText(R.string.deleted_accout));
    }

    @Override
    public void signOut(Context context, TextView textView) {
        AuthUI.getInstance()
                .signOut(context)
                .addOnCompleteListener(task ->
                        textView.setText(R.string.signed_out));
    }
}
