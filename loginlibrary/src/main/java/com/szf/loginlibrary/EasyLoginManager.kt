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
import java.lang.ref.WeakReference


class EasyLoginManager private constructor(context: Context) {

    enum class LoginType {
        Google, Facebook
    }

    private val callbackManager: CallbackManager = CallbackManager.Factory.create()
    private var googleSignInClient: GoogleSignInClient? = null
    private var loginCallback:LoginCallback? = null

    companion object {
        private const val GOOGLE_LOGIN_REQUEST_CODE = 901

        @Volatile
        private var instance: EasyLoginManager? = null

        fun getInstance(context: Context): EasyLoginManager =
            instance ?: synchronized(this) {
                instance ?: EasyLoginManager(context).also { instance = it }
            }

    }

    init {
        registerFacebookCallback()
    }

    fun setLoginCallBack(callback: LoginCallback) {
        this.loginCallback = callback
    }

    fun destroy() {
        this.loginCallback = null
    }

    fun getCallbackManager(): CallbackManager = callbackManager

    // ========================= Facebook 登录 =========================
    fun registerFacebookCallback() {
        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    val token = loginResult.accessToken?.token
                    Log.d("FACEBOOK", "Success accessToken=$token")
                    token?.let { loginCallback?.onSuccess(LoginData.Facebook(it)) }
                }

                override fun onCancel() {
                    Log.d("FACEBOOK", "onCancel")
                    loginCallback?.onCancel()
                }

                override fun onError(error: FacebookException) {
                    Log.e("FACEBOOK", "onError", error)
                    if (error is FacebookAuthorizationException && AccessToken.getCurrentAccessToken() != null) {
                        LoginManager.getInstance().logOut()
                    }
                    loginCallback?.onError(error.localizedMessage ?: "Facebook login error")
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
    fun googleLogin(activity: Activity, clientId: String) {
        googleSignInClient = buildGoogleSignInClient(activity, clientId)
        activity.startActivityForResult(googleSignInClient?.signInIntent, GOOGLE_LOGIN_REQUEST_CODE)
    }

    private fun buildGoogleSignInClient(activity: Activity, clientId: String): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .requestId()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(activity, gso)
    }

    fun googleLogout() {
        googleSignInClient?.signOut()?.addOnCompleteListener {
            Log.d("GOOGLE", "Google 用户已退出")
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            GOOGLE_LOGIN_REQUEST_CODE -> handleGoogleSignInResult(data)
            else -> callbackManager.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleGoogleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            if (task.isSuccessful) {
                val account = task.getResult(ApiException::class.java)
                loginCallback?.onSuccess(LoginData.Google(account))
            }
        } catch (error: ApiException) {
            loginCallback?.onError(error.localizedMessage ?: "Google login error")
        }
    }

    sealed class LoginData {
        data class Google(val account: GoogleSignInAccount) : LoginData()
        data class Facebook(val token: String) : LoginData()
    }

    interface LoginCallback {
        fun onSuccess(data: LoginData)

        fun onCancel()
        fun onError(message: String)
    }
}
