package com.github.sdpcoachme.data

class MockDatabase : Database {
    override fun getData(): String {
        return "Mock data"
    }
}