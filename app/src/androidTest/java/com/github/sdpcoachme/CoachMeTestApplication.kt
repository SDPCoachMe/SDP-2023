package com.github.sdpcoachme

import com.github.sdpcoachme.firebase.auth.MockGoogleAuthenticator

class CoachMeTestApplication : CoachMeApplication() {
    // For DI in testing, add reference to mocks here
    override val googleAuthenticator = MockGoogleAuthenticator()
}