package com.github.sdpcoachme.firebase.auth;

import static android.app.Activity.RESULT_OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

public class RealGoogleAuthenticatorTest {
    @Test
    public void onSignInWithFailedResultCodeExecutesFailCallBack() {
        RealGoogleAuthenticator realGoogleAuthenticator = new RealGoogleAuthenticator();
        AtomicReference<String> result = new AtomicReference<>("");

        realGoogleAuthenticator.onSignInResult(
                new FirebaseAuthUIAuthenticationResult(0, null)
                , (name) -> result.set("success")
                , () -> result.set("failure"));

        assertThat(result.get(), is("failure"));
    }

    @Test
    public void onSignInResultThrowsIfUserIsNull() {
        RealGoogleAuthenticator realGoogleAuthenticator = new RealGoogleAuthenticator();

        Exception error = assertThrows(IllegalStateException.class,
                () -> realGoogleAuthenticator.onSignInResult(
                        new FirebaseAuthUIAuthenticationResult(RESULT_OK, null)
                        , (name) -> {}
                        , () -> {}));
        assertThat(error.getMessage(), is("User is null"));
    }


}
