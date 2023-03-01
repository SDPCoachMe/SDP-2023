package com.github.sdpcoachme.firebase.auth;

import dagger.Component;

@Component(modules = {GoogleAuthModule.class})
public interface GoogleAuthComponent {

    void inject(FirebaseUIActivity firebaseUIActivity);
}
