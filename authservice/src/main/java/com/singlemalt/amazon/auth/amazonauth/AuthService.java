package com.singlemalt.amazon.auth.amazonauth;

import com.amazon.ags.api.AGResponseCallback;
import com.amazon.ags.api.AmazonGamesCallback;
import com.amazon.ags.api.AmazonGamesClient;
import com.amazon.ags.api.AmazonGamesFeature;
import com.amazon.ags.api.AmazonGamesStatus;
import com.amazon.ags.api.player.Player;
import com.amazon.ags.api.player.RequestPlayerResponse;
import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener;
import com.amazon.identity.auth.device.authorization.api.AuthzConstants;
import com.singlemalt.amazon.auth.amazonauth.listeners.LoginListener;
import com.singlemalt.amazon.auth.amazonauth.listeners.TokenListener;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.util.EnumSet;

/**
 * Created by kmiller on 3/25/16.
 */
public class AuthService implements AuthorizationListener, AmazonGamesCallback,
        AGResponseCallback<RequestPlayerResponse> {

    private static final String TAG = AuthService.class.getSimpleName();

    private static final String[] STATUS_VALUES = new String[]
            { "Working", "Success", "Failure", "Cancel"};

    public static final String[] APP_AUTH_SCOPES = new String[]{ "profile" };

    private class AuthRunner implements Runnable {
        private final String TAG = AuthRunner.class.getSimpleName();
        private AmazonAuthorizationManager manager;
        private AuthService authService;
        private Activity activity;
        private EnumSet<AmazonGamesFeature> features;

        public AuthRunner(AuthService authService, Activity activity, EnumSet<AmazonGamesFeature> features) {
            this.authService = authService;
            this.activity = activity;
            this.features = features;
        }

        @Override
        public void run() {
            TokenListener tokenListener = new TokenListener(authService);
            LoginListener loginListener = new LoginListener(authService, tokenListener, manager);

            Log.d(TAG, "Starting");
            manager = new AmazonAuthorizationManager(context, Bundle.EMPTY);
            manager.authorize(APP_AUTH_SCOPES, Bundle.EMPTY, loginListener);

            AmazonGamesClient.initialize(activity, authService, features);
        }
    }

    // android references
    private Context context;
    private Activity activity;

    // game circle client
    private AmazonGamesClient agClient;

    // enums are a pain in the JNI, so using a string
    private String loginStatus;
    private String gcStatus;

    private EnumSet<AmazonGamesFeature> features;
    private Player player;
    private String playerId = "";
    private String oauthToken = "";
    private boolean anonymous = true;

    public AuthService(Context context, Activity activity, boolean achievements, boolean leaderboards,
                       boolean whisperSync) {
        Log.d(TAG, "Starting");

        this.context = context;
        this.activity = activity;
        this.loginStatus = STATUS_VALUES[0];
        this.gcStatus = STATUS_VALUES[0];

        // AmazonGamesClient
        features = EnumSet.noneOf(AmazonGamesFeature.class);

        if(achievements) {
            features.add(AmazonGamesFeature.Achievements);
        }

        if(leaderboards) {
            features.add(AmazonGamesFeature.Leaderboards);
        }

        if(whisperSync) {
            features.add(AmazonGamesFeature.Whispersync);
        }

        new Thread(new AuthRunner(this, activity, features)).start();
    }

    @Override
    public void onCancel(Bundle bundle) {
        Log.d(TAG, "onCancel");

        loginStatus = STATUS_VALUES[3];
    }

    @Override
    public void onSuccess(Bundle bundle) {
        Log.d(TAG, "onSuccess");

        loginStatus = STATUS_VALUES[1];

        if(bundle.getString(AuthzConstants.BUNDLE_KEY.TOKEN.val) != null) {
            oauthToken = bundle.getString(AuthzConstants.BUNDLE_KEY.TOKEN.val);
        }
    }

    @Override
    public void onError(AuthError authError) {
        Log.d(TAG, String.format("onError %s", authError.toString()));

        loginStatus = STATUS_VALUES[2];
    }

    @Override
    public void onServiceReady(AmazonGamesClient amazonGamesClient) {
        agClient = amazonGamesClient;

        if(agClient.getPlayerClient().isSignedIn()) {
            agClient.getPlayerClient().getLocalPlayer((Object[]) null).setCallback(this);
        }
    }

    @Override
    public void onServiceNotReady(AmazonGamesStatus amazonGamesStatus) {
        anonymous = true;
        player = null;
        gcStatus = STATUS_VALUES[2];
    }

    @Override
    public void onComplete(RequestPlayerResponse requestPlayerResponse) {
        if(requestPlayerResponse.isError()) {
            anonymous = true;
            player = null;
            gcStatus = STATUS_VALUES[2];
        } else {
            anonymous = false;
            player = requestPlayerResponse.getPlayer();
            gcStatus = STATUS_VALUES[1];
        }
    }

    public void onPause() {
        if(agClient != null) {
            agClient.release();
        }
    }

    public void onResume() {
        if(agClient != null) {
            agClient = null;
        }

        AmazonGamesClient.initialize(activity, this, features);
    }

    public String getLoginStatus() {
        return loginStatus;
    }

    public String getGcStatus() {
        return gcStatus;
    }

    public String getPlayerName() {
        if(anonymous) {
            return "";
        } else {
            return player.getAlias();
        }
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getOauthToken() {
        return oauthToken;
    }
}
