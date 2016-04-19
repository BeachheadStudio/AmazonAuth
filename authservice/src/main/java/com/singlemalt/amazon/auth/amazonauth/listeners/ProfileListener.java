package com.singlemalt.amazon.auth.amazonauth.listeners;

import android.os.Bundle;
import android.util.Log;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.amazon.identity.auth.device.authorization.api.AuthzConstants;
import com.amazon.identity.auth.device.shared.APIListener;
import com.amazon.insights.core.util.StringUtil;
import com.singlemalt.amazon.auth.amazonauth.AuthInstance;

/**
 * Created by kmiller on 4/15/2016.
 */
public class ProfileListener implements APIListener {
    private AmazonAuthorizationManager manager;
    private APIListener tokenListener;

    public ProfileListener(AmazonAuthorizationManager manager, APIListener tokenListener) {
        this.manager = manager;
        this.tokenListener = tokenListener;
    }

    @Override
    public void onError(AuthError authError) {
        Log.d(AuthInstance.TAG, String.format("LoginListener onError %s", authError.toString()));

        // call service onError
        AuthInstance.getInstance().onError(authError);
    }

    @Override
    public void onSuccess(Bundle bundle) {
        Bundle profileBundle = bundle.getBundle(AuthzConstants.BUNDLE_KEY.PROFILE.val);
        if(profileBundle == null) {
            Log.e(AuthInstance.TAG, "No profile bundle returned");
            onError(new AuthError("No profile bundle returned", AuthError.ERROR_TYPE.ERROR_UNKNOWN));
            return;
        }

        String name = profileBundle.getString(AuthzConstants.PROFILE_KEY.NAME.val);
        String email =profileBundle.getString(AuthzConstants.PROFILE_KEY.EMAIL.val);
        String playerId = profileBundle.getString(AuthzConstants.PROFILE_KEY.USER_ID.val);

        // set playerId
        if(StringUtil.isNullOrEmpty(playerId)) {
            Log.e(AuthInstance.TAG, "The playerId from Amazon is null! Name: " + name + " email: " + email);
        } else {
            Log.d(AuthInstance.TAG, "setting playerId to: " + playerId);
            AuthInstance.getInstance().setPlayerId(playerId);
        }

        // call manager, start token process
        manager.getToken(AuthInstance.APP_AUTH_SCOPES, tokenListener);
    }
}
