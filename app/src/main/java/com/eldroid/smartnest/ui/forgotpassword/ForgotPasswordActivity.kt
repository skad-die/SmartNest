package com.eldroid.smartnest.ui.forgotpassword

import android.content.Intent
import android.os.Bundle
import android.view.View as AndroidView
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.eldroid.smartnest.R
import com.eldroid.smartnest.ui.login.LoginActivity

class ForgotPasswordActivity : AppCompatActivity(), ForgotPasswordContract.View {

    private val presenter: ForgotPasswordContract.Presenter = ForgotPasswordPresenter()

    private lateinit var etEmail: EditText
    private lateinit var btnSendReset: Button
    private lateinit var btnBackToLogin: Button
    private lateinit var tvForgotPasswordError: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

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

    override fun showError(message: String) {
        tvForgotPasswordError.text = message
        tvForgotPasswordError.setTextColor(0xFFD32F2F.toInt())
    }

    override fun clearError() {
        tvForgotPasswordError.text = ""
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
        btnSendReset.isEnabled = false
    }

    override fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }
}