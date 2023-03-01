package com.github.sdpcoachme.firebase.auth;

import android.content.Context;

import dagger.Module;
import dagger.Provides;

@Module
public class GoogleAuthModule {

    @Provides
    public GoogleAuthenticator provideGoogleAuthenticator() {
        return new RealGoogleAuthenticator();
    }
}
