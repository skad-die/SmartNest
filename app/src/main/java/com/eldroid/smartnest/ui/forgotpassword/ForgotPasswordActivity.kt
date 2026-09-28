package com.eldroid.smartnest.ui.forgotpassword

import android.os.Bundle
import android.view.View as AndroidView
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.eldroid.smartnest.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ForgotPasswordActivity : AppCompatActivity(), ForgotPasswordContract.View {

    private val presenter: ForgotPasswordContract.Presenter = ForgotPasswordPresenter()

    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var btnSendReset: Button
    private lateinit var btnBackToLogin: Button
    private lateinit var tvForgotPasswordError: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        btnSendReset = findViewById(R.id.btnSendReset)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)
        tvForgotPasswordError = findViewById(R.id.tvForgotPasswordError)
        progressBar = findViewById(R.id.progressBar)

        presenter.attachView(this)

        btnSendReset.setOnClickListener {
            hideKeyboard()
            presenter.onSendResetClicked(etEmail.text.toString())
        }

        btnBackToLogin.setOnClickListener {
            presenter.onBackToLoginClicked()
        }
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    override fun showEmailError(message: String?) {
        tilEmail.error = message
    }

    override fun clearAllErrors() {
        tilEmail.error = null
        tvForgotPasswordError.text = ""
        tvForgotPasswordError.visibility = AndroidView.GONE
    }

    override fun showGeneralError(message: String) {
        tvForgotPasswordError.setTextColor(0xFFD32F2F.toInt())
        tvForgotPasswordError.text = message
        tvForgotPasswordError.visibility = AndroidView.VISIBLE
    }

    override fun showLoading() {
        progressBar.visibility = AndroidView.VISIBLE
        btnSendReset.isEnabled = false
    }

    override fun hideLoading() {
        progressBar.visibility = AndroidView.GONE
        btnSendReset.isEnabled = true
    }

    override fun showRequestSentMessage() {
        tvForgotPasswordError.setTextColor(0xFF2E7D32.toInt())
        tvForgotPasswordError.text = getString(R.string.reset_link_sent_message)
        tvForgotPasswordError.visibility = AndroidView.VISIBLE
        btnSendReset.isEnabled = false
    }

    override fun navigateToLogin() {
        finish()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }
}