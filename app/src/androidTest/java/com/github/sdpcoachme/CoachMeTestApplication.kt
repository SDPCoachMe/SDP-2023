package com.github.sdpcoachme

import com.github.sdpcoachme.firebase.auth.Authenticator
import com.github.sdpcoachme.firebase.auth.MockAuthenticator

class CoachMeTestApplication : CoachMeApplication() {
    // For DI in testing, add reference to mocks here
    override val authenticator: Authenticator = MockAuthenticator()
}