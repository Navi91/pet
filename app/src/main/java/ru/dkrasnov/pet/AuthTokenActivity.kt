package ru.dkrasnov.pet

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.yandex.authsdk.YandexAuthException
import com.yandex.authsdk.YandexAuthLoginOptions
import com.yandex.authsdk.YandexAuthOptions
import com.yandex.authsdk.YandexAuthSdk
import ru.mail.auth.sdk.AuthError
import ru.mail.auth.sdk.AuthResult
import ru.mail.auth.sdk.MailRuAuthSdk
import ru.mail.auth.sdk.MailRuCallback
import ru.mail.auth.sdk.api.token.OAuthTokensResult

class AuthTokenActivity: AppCompatActivity() {

    private val yandexSdk: YandexAuthSdk by lazy {
        YandexAuthSdk(
            this,
            YandexAuthOptions.Builder(this)
                .enableLogging()
                .build()
        )
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MailRuAuthSdk.initialize(this)
        MailRuAuthSdk.getInstance().isDebugEnabled = true
        MailRuAuthSdk.getInstance().setRequestCodeOffset(300)

        val am = AccountManager.get(this)

        Log.d(
            "AccountManager",
            "Accounts: ${
                am.getAccountsByType("com.yandex.passport").map { "Account ${it.name} ${it.type}" }
            }"
        )
        Log.d("AccountManager", "Authenticators: ${am.authenticatorTypes.map {
            "\n${it.type} ${it.accountPreferencesId} ${it.packageName} ${it.customTokens}"
        }}")

        val intent = AccountManager.newChooseAccountIntent(
            null,
            null,
            arrayOf("com.google", "com.yandex.passport", "ru.mail"),
//            arrayOf("com.google", "com.yandex.passport", "ru.mail"),
            false,
            null,
            null,
            null,
            null
        )

        startActivityForResult(intent, ACCOUNT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d("AccountManager", "requestCode: $requestCode resultCode: $resultCode data: $data")

        when (requestCode) {
            ACCOUNT_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    val extras = data?.extras

                    val account = Account(
                        extras?.getString(AccountManager.KEY_ACCOUNT_NAME),
                        extras?.getString(AccountManager.KEY_ACCOUNT_TYPE)
                    )

                    when (account.type) {
                        "com.yandex.passport" -> {
                            takeYandexToken(account)
                        }
                        "ru.mail" -> {
                            takeMailRuToken()
                        }
                        else -> {
                            takeAccountManagerToken(account)
                        }
                    }
                }
            }
            YANDEX_REQUEST_CODE -> {
                try {
                    val yandexAuthToken = yandexSdk.extractToken(resultCode, data)

                    Log.d("AccountManager", "yandexAuthToken: $yandexAuthToken")
                } catch (e: YandexAuthException) {
                    Log.e("AccountManager", "yandexAuthToken error: $e")
                }
            }
            MailRuAuthSdk.getInstance().loginRequestCode -> {
                MailRuAuthSdk.getInstance().handleActivityResult(
                    requestCode,
                    resultCode,
                    data,
                    object : MailRuCallback<AuthResult, AuthError> {

                        override fun onResult(result: AuthResult) {
                            Log.d("AccountManager", "MailRu auth result: $result")

                            MailRuAuthSdk.getInstance().requestOAuthTokens(
                                result,
                                object : MailRuCallback<OAuthTokensResult, Int> {
                                    override fun onResult(result: OAuthTokensResult) {
                                        Log.d("AccountManager", "MailRu oauth result: $result")
                                    }

                                    override fun onError(error: Int) {
                                        Log.d("AccountManager", "MailRu oauth error: $error")
                                    }
                                }
                            )
                        }

                        override fun onError(error: AuthError) {
                            Log.d("AccountManager", "MailRu auth error: $error")
                        }
                    }
                )
            }
        }
    }

    private fun takeAccountManagerToken(account: Account) {
        val am = AccountManager.get(this)

        Log.d("AccountManager", "Selected account: $account")

        val options = Bundle()
            .apply {
                putString("clientSecret", "929dd0eec314496586ef8273c52eaf27")
            }

        am.getAuthToken(
            account,
            "oauth2:https://www.googleapis.com/auth/userinfo.email",
            options,
            this,
            { result ->
                Log.d("AccountManager", "Result callback: $result")
                // Get the result of the operation from the AccountManagerFuture.
                try {
                    val bundle: Bundle = result.result

                    Log.d("AccountManager", "result bundle: $bundle")

                    // The token is a named value in the bundle. The name of the value
                    // is stored in the constant AccountManager.KEY_AUTHTOKEN.
                    val token: String? = bundle.getString(AccountManager.KEY_AUTHTOKEN)

                    Log.d("AccountManager", "Token: $token")
                } catch (e: Exception) {
                    Log.d("AccountManager", "Get result error $e")
                }
            },
            null
        )
    }

    private fun takeYandexToken(account: Account) {
        val options = YandexAuthLoginOptions.Builder()
            .setLoginHint(account.name)
            .build()

        startActivityForResult(yandexSdk.createLoginIntent(options), YANDEX_REQUEST_CODE)
    }

    private fun takeMailRuToken() {
        MailRuAuthSdk.getInstance().startLogin(this)
    }

    companion object {

        const val ACCOUNT_REQUEST_CODE = 100
        const val YANDEX_REQUEST_CODE = 200

        fun createIntent(context: Context): Intent =
            Intent(context, AuthTokenActivity::class.java)
    }
}