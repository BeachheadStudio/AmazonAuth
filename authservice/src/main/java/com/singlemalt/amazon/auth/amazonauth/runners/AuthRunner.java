package com.singlemalt.amazon.auth.amazonauth.runners;

import android.os.Bundle;
import android.util.Log;

import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.singlemalt.amazon.auth.amazonauth.AuthInstance;
import com.singlemalt.amazon.auth.amazonauth.AuthServiceActivity;
import com.singlemalt.amazon.auth.amazonauth.listeners.LoginListener;
import com.singlemalt.amazon.auth.amazonauth.listeners.ProfileListener;
import com.singlemalt.amazon.auth.amazonauth.listeners.TokenListener;

/**
 * Created by kmiller on 4/14/2016.
 */
public class AuthRunner implements Runnable {
    private AuthServiceActivity authServiceActivity;

    public AuthRunner(AuthServiceActivity authServiceActivity) {
        this.authServiceActivity = authServiceActivity;
    }

    @Override
    public void run() {
        Log.d(AuthInstance.TAG, "Starting Amazon login");
        AmazonAuthorizationManager manager = new AmazonAuthorizationManager(
                authServiceActivity.getApplicationContext(), Bundle.EMPTY);

        TokenListener tokenListener = new TokenListener();
        ProfileListener profileListener = new ProfileListener(manager, tokenListener);
        LoginListener loginListener = new LoginListener(profileListener, manager);

        manager.authorize(AuthInstance.APP_AUTH_SCOPES, Bundle.EMPTY, loginListener);
    }
}
