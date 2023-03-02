package com.github.sdpcoachme.firebase.auth;

import dagger.Module;
import dagger.Provides;

/**
 * Module that provides the implementation of the GoogleAuthenticator
 */
@Module
public class GoogleAuthModule {

    /**
     * Provides the implementation of the GoogleAuthenticator
     *
     * @return the implementation of the GoogleAuthenticator
     */
    @Provides
    public GoogleAuthenticator provideGoogleAuthenticator() {
        return new RealGoogleAuthenticator();
    }
}
