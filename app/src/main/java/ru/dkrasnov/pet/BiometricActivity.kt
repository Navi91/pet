package ru.dkrasnov.pet

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class BiometricActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.a_biometric)

        setupAuth()
        setupBroadcastReceiver()
    }

    override fun onResume() {
        super.onResume()

        logInfo()
    }

    private fun setupAuth() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
//                    Toast.makeText(
//                        applicationContext,
//                        "Authentication error: $errString", Toast.LENGTH_SHORT
//                    )
//                        .show()

                    Log.d(LOG_TAG, "Authentication error: $errString")
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
//                    Toast.makeText(
//                        applicationContext,
//                        "Authentication succeeded!", Toast.LENGTH_SHORT
//                    )
//                        .show()
                    Log.d(LOG_TAG, "Authentication succeeded!")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
//                    Toast.makeText(
//                        applicationContext, "Authentication failed",
//                        Toast.LENGTH_SHORT
//                    )
//                        .show()
                    Log.d(LOG_TAG, "Authentication failed")
                }
            })

//        val promptInfo = BiometricPrompt.PromptInfo.Builder()
//            .setTitle("Biometric login for my app")
//            .setSubtitle("Log in using your biometric credential")
//            .setNegativeButtonText("Use account password")
//            .setAllowedAuthenticators(BIOMETRIC_STRONG)
////            .setDeviceCredentialAllowed(true)
////            .setConfirmationRequired(false)
//            .build()

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for my app")
            .setSubtitle("Log in using your biometric credential")
            .setDeviceCredentialAllowed(true)
            .build()

        BiometricManager.from(this).canAuthenticate()

        // Prompt appears when user clicks "Log in".
        // Consider integrating with the keystore to unlock cryptographic operations,
        // if needed by your app.
        findViewById<TextView>(R.id.textView).setOnClickListener {

            when {
                checkBiometricChanged() -> {
                    //TODO
                }
                BiometricManager.from(this)
                    .canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS -> {
                    biometricPrompt.authenticate(promptInfo)
                }
                !getSystemService(KeyguardManager::class.java).isDeviceSecure -> {
                    startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                }
                else -> {
                    val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG)
                    }

                    startActivity(enrollIntent)
                }
            }
        }
    }

    private fun logInfo() {

        val keyguardManager = getSystemService(KeyguardManager::class.java)

        val keyguardManagerInfo = "Keyguard manager params:\n" +
                "isDeviceLocked: ${keyguardManager.isDeviceLocked}\n" +
                "isDeviceSecure: ${keyguardManager.isDeviceSecure}\n" +
                "isKeyguardLocked: ${keyguardManager.isKeyguardLocked}\n" +
                "isKeyguardSecure: ${keyguardManager.isKeyguardSecure}"

        val biometricManager = BiometricManager.from(this)

        val biometricManagerInfo = "Biometric manager info:\n" +
                "BIOMETRIC_STRONG: ${
                    biometricManager.canAuthenticate(BIOMETRIC_STRONG).statusToText()
                }\n" +
                "BIOMETRIC_WEAK: ${
                    biometricManager.canAuthenticate(BIOMETRIC_WEAK).statusToText()
                }\n" +
                "DEVICE_CREDENTIAL: ${
                    biometricManager.canAuthenticate(DEVICE_CREDENTIAL).statusToText()
                }\n" +
                "BIOMETRIC_STRONG or DEVICE_CREDENTIAL: ${
                    biometricManager.canAuthenticate(
                        BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                    ).statusToText()
                }\n" +
                "BIOMETRIC_WEAK or DEVICE_CREDENTIAL: ${
                    biometricManager.canAuthenticate(
                        BIOMETRIC_WEAK or DEVICE_CREDENTIAL
                    ).statusToText()
                }\n" +
                "BIOMETRIC_WEAK or BIOMETRIC_STRONG: ${
                    biometricManager.canAuthenticate(
                        BIOMETRIC_WEAK or BIOMETRIC_STRONG
                    ).statusToText()
                }\n"

        val info = "$keyguardManagerInfo\n\n$biometricManagerInfo"

//        Log.d(LOG_TAG, info)

        findViewById<TextView>(R.id.textView).text = info
    }

    private fun Int.statusToText(): String =
        when (this) {
            BiometricManager.BIOMETRIC_SUCCESS -> "BIOMETRIC_SUCCESS"
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "BIOMETRIC_STATUS_UNKNOWN"
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "BIOMETRIC_ERROR_UNSUPPORTED"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "BIOMETRIC_ERROR_HW_UNAVAILABLE"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "BIOMETRIC_ERROR_NONE_ENROLLED"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "BIOMETRIC_ERROR_NO_HARDWARE"
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED"
            else -> "UNKNOWN"
        }

    private fun setupBroadcastReceiver() {
        val receiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(LOG_TAG, "Phone unlocked")
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
        }

        registerReceiver(receiver, intentFilter)
    }

    private fun checkBiometricChanged(): Boolean {
        val keystore = KeyStore.getInstance("AndroidKeyStore")
        keystore.load(null)

        if (!keystore.containsAlias(KEY_NAME)) {
            Log.d(LOG_TAG, "Set key")

            keystore.setKeyEntry(
                KEY_NAME,
                createSecretKey(),
                null,
                null
            )
        } else {
            Log.d(LOG_TAG, "Key already exists")
        }

        val secretKey = keystore.getKey(KEY_NAME, null) as SecretKey

        val cipher = Cipher.getInstance(
            "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"
        )

        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        } catch (e: KeyPermanentlyInvalidatedException) {
            Log.d(LOG_TAG, "Init cipher error: $e")

            // По этой ошибке понимаем, что биометрия изменилась, значит ключ необходимо пересоздать

            keystore.deleteEntry(KEY_NAME) // на самом деле лишний вызов, setKeyEntry перезатирает ключ
            keystore.setKeyEntry(
                KEY_NAME,
                createSecretKey(),
                null,
                null
            )
            return true
        }

        return false
    }

    private fun createSecretKey(): SecretKey {
        val keyGenParameterSpec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            KeyGenParameterSpec.Builder(
                KEY_NAME,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .build()
        } else {
            TODO("VERSION.SDK_INT < N")
        }
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        keyGenerator.init(keyGenParameterSpec)

        return keyGenerator.generateKey()
    }

    companion object {

        private const val LOG_TAG = "BiometricActivity"

        private const val KEY_NAME = "KEY_NAME"

        fun createIntent(context: Context): Intent =
            Intent(context, BiometricActivity::class.java)
    }
}