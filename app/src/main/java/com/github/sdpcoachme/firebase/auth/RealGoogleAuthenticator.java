package com.github.sdpcoachme.firebase.auth;

import static android.app.Activity.RESULT_OK;

import android.content.Context;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

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

         try {
             signInLauncher.launch(signInIntent);
         } catch (Exception e) {
             System.out.println("My Error: " + e.getMessage());
         }
    }

    @Override
    public void onSignInResult(FirebaseAuthUIAuthenticationResult result, Consumer<String> onSuccess, Runnable onFailure) {
        if (result.getResultCode() == RESULT_OK) {
            // Successfully signed in
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user == null) throw new IllegalStateException("User is null");

            onSuccess.accept(user.getEmail());

        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
            onFailure.run();
        }
    }

    @Override
    public void signIn(ActivityResultLauncher<Intent> signInLauncher) {
        createSignInIntent(signInLauncher);
    }

    @Override
    public void delete(Context context, Runnable onComplete) {
        AuthUI.getInstance()
                .delete(context)
                .addOnCompleteListener(task ->
                        onComplete.run());
    }

    @Override
    public void signOut(Context context, Runnable onComplete) {
        AuthUI.getInstance()
                .signOut(context)
                .addOnCompleteListener(task ->
                        onComplete.run());
    }
}
