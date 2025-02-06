package com.szf.loginlibrary;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;

public class EasyLoginFaceBookSdkManager {

    private static EasyLoginFaceBookSdkManager mInstance;
    private CallbackManager callbackManager;

    public static EasyLoginFaceBookSdkManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new EasyLoginFaceBookSdkManager(context.getApplicationContext());
        }
        return mInstance;
    }

    public EasyLoginFaceBookSdkManager(Context context) {
//        FacebookSdk.sdkInitialize(context);
        callbackManager = CallbackManager.Factory.create();
    }

    public CallbackManager getCallbackManager() {
        return callbackManager;
    }

    /**
     * facebook注册回调
     *
     * @param callBack
     */
    public void registerCallback(final FacebookLoginCallBack callBack) {
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                AccessToken accessToken = loginResult.getAccessToken();
                if (accessToken != null) {
                    if (callBack != null) {
                        callBack.onSuccess(accessToken.getToken());
                    }
                    Log.e("FACEBOOK", "Success accessToken=" + accessToken);
                }
            }

            @Override
            public void onCancel() {
                if (callBack != null) {
                    callBack.onCancel();
                }
                Log.e("FACEBOOK", "onCancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.e("FACEBOOK", "onError=");
                if (error instanceof FacebookAuthorizationException) {
                    if (AccessToken.getCurrentAccessToken() != null) {
                        LoginManager.getInstance().logOut();
                    }
                }
                String message = error.getMessage();
                try {
                    JSONObject jsonObject = new JSONObject(message);
                    String errorMessage = jsonObject.optString("errorMessage");
                    int errorCode = jsonObject.optInt("errorCode");
                    if (callBack != null) {
                        callBack.onError(errorMessage + "(" + errorCode + ")");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    if (callBack != null) {
                        callBack.onError("");
                    }
                }
            }
        });
    }

    public void login(Activity context, boolean IsLogin) {
//        ShareUitls.putBoolean(context, "FaceBookIsLogin", IsLogin);
        //添加public_profile和email权限
        LoginManager.getInstance().logInWithReadPermissions(context, Collections.singletonList("public_profile"));
    }

    /**
     * 注销
     * 如果注销的时候，不调用这个方法，当换一个新的账号登录的时候，会出现 UserData logged in as different Facebook user 异常
     */
    public void logout() {
        LoginManager.getInstance().logOut();
    }


    public interface FacebookLoginCallBack {
        void onSuccess(String accessToken);

        void onCancel();

        void onError(String message);
    }

}

