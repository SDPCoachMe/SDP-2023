package com.github.sdpcoachme.firebase.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.github.sdpcoachme.R;

import javax.inject.Inject;

public class FirebaseUIActivity extends AppCompatActivity {

    //inject instance of GoogleAuthenticator
    @Inject
    GoogleAuthenticator googleAuthenticator;


    // See: https://developer.android.com/training/basics/intents/result
    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            res -> googleAuthenticator.onSignInResult(res, findViewById(R.id.user_email))
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_ui);

        DaggerGoogleAuthComponent.builder()
                .googleAuthModule(new GoogleAuthModule())
                .build()
                .inject(this);
    }

//    public void createSignInIntent() {
//        // Choose authentication providers
//        List<AuthUI.IdpConfig> providers = Arrays.asList(
//                new AuthUI.IdpConfig.GoogleBuilder().build());
//
//        // Create and launch sign-in intent
//        Intent signInIntent = AuthUI.getInstance()
//                .createSignInIntentBuilder()
//                .setAvailableProviders(providers)
//                .build();
//        signInLauncher.launch(signInIntent);
//    }

//    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
//        IdpResponse response = result.getIdpResponse();
//        TextView textView = findViewById(R.id.user_email);
//
//        if (result.getResultCode() == RESULT_OK) {
//            // Successfully signed in
//            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//            // ...
//            assert user != null;
//            textView.setText(String.format("%s %s", getString(R.string.signed_in_as), user.getEmail()));
//
//        } else {
//            textView.setText(R.string.sign_in_failed);
//            // Sign in failed. If response is null the user canceled the
//            // sign-in flow using the back button. Otherwise check
//            // response.getError().getErrorCode() and handle the error.
//            // ...
//        }
//    }

//    public void signOut() {
//        TextView textView = findViewById(R.id.user_email);
//        AuthUI.getInstance()
//                .signOut(this)
//                .addOnCompleteListener(task -> {
//                    // ...
//                    textView.setText(R.string.signed_out);
//                });
//    }

    public void deleteGoogleAccount(View view) {
        googleAuthenticator.delete(this, findViewById(R.id.user_email));
//        delete();
    }

    public void signIntoGoogleAccount(View view) {
        googleAuthenticator.signIn(signInLauncher);
//        createSignInIntent();
    }

    public void signOutOfGoogleAccount(View view) {
        googleAuthenticator.signOut(this, findViewById(R.id.user_email));
//        signOut();
    }


//    public void delete() {
//        AuthUI.getInstance()
//                .delete(this)
//                .addOnCompleteListener(task -> {
//                    // ...
//                });
//    }

//    public void emailLink() {
//        ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder()
//                .setAndroidPackageName(
//                        /* yourPackageName= */ "com.github.sdpcoachme",
//                        /* installIfNotAvailable= */ true,
//                        /* minimumVersion= */ null)
//                .setHandleCodeInApp(true) // This must be set to true
//                .setUrl(getString(R.string.firebase_sign_in_domain)) // This URL needs to be whitelisted
//                .build();
//
//        List<AuthUI.IdpConfig> providers = Collections.singletonList(
//                new AuthUI.IdpConfig.EmailBuilder()
//                        .enableEmailLinkSignIn()
//                        .setActionCodeSettings(actionCodeSettings)
//                        .build()
//        );
//        Intent signInIntent = AuthUI.getInstance()
//                .createSignInIntentBuilder()
//                .setAvailableProviders(providers)
//                .build();
//        signInLauncher.launch(signInIntent);
//    }
//
//    public void catchEmailLink() {
//        List<AuthUI.IdpConfig> providers = Collections.emptyList();
//
//        if (AuthUI.canHandleIntent(getIntent())) {
//            if (getIntent().getExtras() == null) {
//                return;
//            }
//            String link = getIntent().getExtras().getString("email_link_sign_in");
//            if (link != null) {
//                Intent signInIntent = AuthUI.getInstance()
//                        .createSignInIntentBuilder()
//                        .setEmailLink(link)
//                        .setAvailableProviders(providers)
//                        .build();
//                signInLauncher.launch(signInIntent);
//            }
//        }
//    }
}
