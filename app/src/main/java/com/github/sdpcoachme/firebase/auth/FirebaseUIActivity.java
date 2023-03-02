package com.github.sdpcoachme.firebase.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

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
            res -> googleAuthenticator.onSignInResult(
                    res
                    , email -> ((TextView) findViewById(R.id.sign_in_info))
                            .setText(String.format(getString(R.string.signed_in_as), email))
                    , () -> ((TextView) findViewById(R.id.sign_in_info))
                            .setText(R.string.sign_in_failed)
            )
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

    public void deleteGoogleAccount(View view) {
        googleAuthenticator.delete(this, () -> ((TextView) findViewById(R.id.sign_in_info)).setText(R.string.deleted_accout));
    }

    public void signIntoGoogleAccount(View view) {
        googleAuthenticator.signIn(signInLauncher);
    }

    public void signOutOfGoogleAccount(View view) {
        googleAuthenticator.signOut(this, () -> ((TextView) findViewById(R.id.sign_in_info)).setText(R.string.signed_out));
    }
}
