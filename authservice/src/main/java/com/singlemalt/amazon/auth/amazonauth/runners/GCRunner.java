package com.singlemalt.amazon.auth.amazonauth.runners;

import android.util.Log;

import com.amazon.ags.api.AmazonGamesClient;
import com.singlemalt.amazon.auth.amazonauth.AuthInstance;
import com.singlemalt.amazon.auth.amazonauth.AuthServiceActivity;

/**
 * Created by kmiller on 4/14/2016.
 */
public class GCRunner implements Runnable {
    private AuthServiceActivity authServiceActivity;

    public GCRunner(AuthServiceActivity authServiceActivity) {
        this.authServiceActivity = authServiceActivity;
    }

    @Override
    public void run() {
        Log.d(AuthInstance.TAG, "Starting GameCircle login");

        try {
            AmazonGamesClient.initialize(authServiceActivity, authServiceActivity,
                    AuthInstance.getInstance().getFeatures());
        } catch (Exception e) {
            Log.e(AuthInstance.TAG, "Gamecircle launch failed ", e);
            AuthInstance.getInstance().setGcStatus(AuthInstance.Status.Success);
            authServiceActivity.finish();
        }
    }
}
