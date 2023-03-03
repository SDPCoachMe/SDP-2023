package com.github.sdpcoachme.firebase.auth

import dagger.Component

/**
 * Component that provides the dependencies for the GoogleAuthenticator
 */
@Component(modules = [GoogleAuthModule::class])
interface GoogleAuthComponent {
    /**
     * Injects the dependencies into the given activity
     *
     * @param firebaseUIActivity the activity to inject into
     */
    fun inject(firebaseUIActivity: FirebaseAuthActivity?)
}