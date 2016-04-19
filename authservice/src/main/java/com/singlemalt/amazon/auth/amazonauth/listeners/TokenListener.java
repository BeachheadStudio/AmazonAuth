package com.singlemalt.amazon.auth.amazonauth.listeners;

import android.os.Bundle;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AuthzConstants;
import com.amazon.identity.auth.device.shared.APIListener;
import com.singlemalt.amazon.auth.amazonauth.AuthInstance;
import com.singlemalt.amazon.auth.amazonauth.AuthServiceActivity;

import android.util.Log;

/**
 * Created by kmiller on 3/25/16.
 */
public class TokenListener implements APIListener {
    public TokenListener() {
        Log.d(AuthInstance.TAG, "TokenListener init");
    }

    @Override
    public void onSuccess(Bundle bundle) {
        Log.d(AuthInstance.TAG, "TokenListener onSuccess");
        AuthInstance.getInstance().setOauthToken(bundle.getString(AuthzConstants.BUNDLE_KEY.TOKEN.val));

        bundle.putBoolean("isToken", true);
        AuthInstance.getInstance().onSuccess(bundle);
    }

    @Override
    public void onError(AuthError authError) {
        Log.d(AuthInstance.TAG, String.format("TokenListener onError %s", authError.toString()));

        // call service onError
        AuthInstance.getInstance().onError(authError);
    }
}
