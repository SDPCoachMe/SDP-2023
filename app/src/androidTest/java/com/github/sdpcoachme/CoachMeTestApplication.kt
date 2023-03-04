package com.github.sdpcoachme

import com.github.sdpcoachme.data.MockDatabase

class CoachMeTestApplication : CoachMeApplication() {
    override val database = MockDatabase()
}