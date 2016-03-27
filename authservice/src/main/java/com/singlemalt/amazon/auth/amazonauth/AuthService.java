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
import android.os.Bundle;
import android.util.Log;

import com.unity3d.player.UnityPlayer;

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
        private EnumSet<AmazonGamesFeature> features;

        public AuthRunner(AuthService authService, EnumSet<AmazonGamesFeature> features) {
            this.authService = authService;
            this.features = features;
        }

        @Override
        public void run() {
            TokenListener tokenListener = new TokenListener(authService);
            LoginListener loginListener = new LoginListener(authService, tokenListener, manager);

            Log.d(TAG, "Starting");
            manager = new AmazonAuthorizationManager(
                    UnityPlayer.currentActivity.getApplicationContext(), Bundle.EMPTY);
            manager.authorize(APP_AUTH_SCOPES, Bundle.EMPTY, loginListener);

            AmazonGamesClient.initialize(UnityPlayer.currentActivity, authService, features);
        }
    }

    // singleton methods
    private static AuthService instance;

    public static AuthService getInstance() {
        if(instance == null) {
            instance = new AuthService();
        }

        return instance;
    }

    // game circle client
    private AmazonGamesClient agClient;

    // enums are a pain in the JNI, so using a string
    private String loginStatus;
    private String gcStatus;

    // Amazon parameters
    private EnumSet<AmazonGamesFeature> features;
    private Player player;
    private String playerId = null;
    private String oauthToken = "";
    private boolean anonymous = true;

    private AuthService() {

    }

    public void Init(boolean achievements, boolean leaderboards, boolean whisperSync) {
        Log.d(TAG, "Starting");

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

        new Thread(new AuthRunner(this, features)).start();
    }

    @Override
    public void onCancel(Bundle bundle) {
        Log.d(TAG, "onCancel");

        loginStatus = STATUS_VALUES[3];
        checkStatus();
    }

    @Override
    public void onSuccess(Bundle bundle) {
        Log.d(TAG, "onSuccess");

        loginStatus = STATUS_VALUES[1];
        oauthToken = bundle.getString(AuthzConstants.BUNDLE_KEY.TOKEN.val);

        checkStatus();
    }

    @Override
    public void onError(AuthError authError) {
        Log.d(TAG, String.format("onError %s", authError.toString()));

        loginStatus = STATUS_VALUES[2];
        checkStatus();
    }

    @Override
    public void onServiceReady(AmazonGamesClient amazonGamesClient) {
        agClient = amazonGamesClient;

        if(agClient.getPlayerClient().isSignedIn()) {
            agClient.getPlayerClient().getLocalPlayer((Object[]) null).setCallback(this);
        } else {
            gcStatus = STATUS_VALUES[3];
            checkStatus();
        }
    }

    @Override
    public void onServiceNotReady(AmazonGamesStatus amazonGamesStatus) {
        gcStatus = STATUS_VALUES[2];
        checkStatus();
    }

    @Override
    public void onComplete(RequestPlayerResponse requestPlayerResponse) {
        if(requestPlayerResponse.isError()) {
            gcStatus = STATUS_VALUES[2];
        } else {
            player = requestPlayerResponse.getPlayer();
            gcStatus = STATUS_VALUES[1];
        }

        checkStatus();
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

        AmazonGamesClient.initialize(UnityPlayer.currentActivity, this, features);
    }

    public String getPlayerName() {
        if(anonymous) {
            return "";
        } else {
            return player.getAlias();
        }
    }

    public void setPlayerId(String playerId) {
        if(this.playerId != null) {
            if(!this.playerId.equals(playerId)) {
                this.playerId = playerId;
                UnityPlayer.UnitySendMessage("Main Camera", "PlayerChange", "true");
            }
        } else {
            this.playerId = playerId;
        }
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getOauthToken() {
        return oauthToken;
    }

    private void checkStatus() {
        if(loginStatus.equals(STATUS_VALUES[0]) || gcStatus.equals(STATUS_VALUES[0])) {
            // not ready to update unity
            return;
        }

        if(loginStatus.equals(STATUS_VALUES[1]) && gcStatus.equals(STATUS_VALUES[1])) {
            anonymous = false;

            UnityPlayer.UnitySendMessage("Main Camera", "LoginResult", STATUS_VALUES[1]);
        } else if(loginStatus.equals(STATUS_VALUES[3])) {
            anonymous = true;
            player = null;

            UnityPlayer.UnitySendMessage("Main Camera", "LoginResult", STATUS_VALUES[3]);
        } else if(loginStatus.equals(STATUS_VALUES[2]) || gcStatus.equals(STATUS_VALUES[2])) {
            anonymous = true;
            player = null;

            UnityPlayer.UnitySendMessage("Main Camera", "LoginResult", STATUS_VALUES[2]);
        }
    }

}
