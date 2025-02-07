package com.szf.loginlibrary

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import org.json.JSONException
import org.json.JSONObject

class EasyLoginManager private constructor(private val context: Context) {

    private val GOOGLE_LOGIN_REQUEST_CODE = 901

    private val callbackManager: CallbackManager = CallbackManager.Factory.create()
    private var googleSignInClient: GoogleSignInClient? = null
    private var googleLoginCallback: GoogleLoginCallback? = null

    companion object {
        @Volatile
        private var instance: EasyLoginManager? = null

        fun getInstance(context: Context): EasyLoginManager {
            return instance ?: synchronized(this) {
                instance ?: EasyLoginManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    fun getCallbackManager(): CallbackManager = callbackManager

    // ========================= Facebook 登录 =========================
    fun registerFacebookCallback(callback: FacebookLoginCallback?) {
        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    val accessToken = loginResult.accessToken
                    if (accessToken != null) {
                        callback?.onSuccess(accessToken.token)
                        Log.e("FACEBOOK", "Success accessToken=$accessToken")
                    }
                }

                override fun onCancel() {
                    callback?.onCancel()
                    Log.e("FACEBOOK", "onCancel")
                }

                override fun onError(error: FacebookException) {
                    Log.e("FACEBOOK", "onError=", error)
                    if (error is FacebookAuthorizationException && AccessToken.getCurrentAccessToken() != null) {
                        LoginManager.getInstance().logOut()
                    }
                    val message = error.message ?: ""
                    try {
                        val jsonObject = JSONObject(message)
                        val errorMessage = jsonObject.optString("errorMessage")
                        val errorCode = jsonObject.optInt("errorCode")
                        callback?.onError("$errorMessage($errorCode)")
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        callback?.onError("Facebook 登录失败")
                    }
                }
            })
    }

    fun facebookLogin(activity: Activity) {
        LoginManager.getInstance().logInWithReadPermissions(activity, listOf("public_profile"))
    }

    fun facebookLogout() {
        LoginManager.getInstance().logOut()
    }

    // ========================= Google 登录 =========================
    fun googleLogin(activity: Activity, clientId: String, callback: GoogleLoginCallback) {
        googleLoginCallback = callback
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .requestId()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(activity, gso)
        val googleSignInIntent = googleSignInClient?.signInIntent
        activity.startActivityForResult(googleSignInIntent, GOOGLE_LOGIN_REQUEST_CODE)
    }

    fun googleLogout() {
        googleSignInClient?.signOut()?.addOnCompleteListener {
            Log.e("GOOGLE", "Google 用户已退出")
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == GOOGLE_LOGIN_REQUEST_CODE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                googleLoginCallback?.onSuccess(account)
            } catch (e: ApiException) {
                googleLoginCallback?.onFailure(e)
            }
        }
        if (callbackManager != null) {
            callbackManager.onActivityResult(requestCode, resultCode, data)
        }
    }

    // ========================= 接口回调 =========================
    interface FacebookLoginCallback {
        fun onSuccess(accessToken: String)
        fun onCancel()
        fun onError(message: String)
    }

    interface GoogleLoginCallback {
        fun onSuccess(account: GoogleSignInAccount)
        fun onFailure(e: Exception)
    }
}
