package com.eldroid.smartnest.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View as AndroidView
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.eldroid.smartnest.R
import com.eldroid.smartnest.ui.dashboard.DashboardActivity
import com.eldroid.smartnest.ui.forgotpassword.ForgotPasswordActivity
import com.eldroid.smartnest.ui.register.RegisterActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity(), LoginContract.View {

    private val presenter: LoginContract.Presenter = LoginPresenter()

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText

    private lateinit var btnLogin: Button
    private lateinit var btnGoToRegister: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvLoginError: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)

        btnLogin = findViewById(R.id.btnLogin)
        btnGoToRegister = findViewById(R.id.btnGoToRegister)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvLoginError = findViewById(R.id.tvLoginError)
        progressBar = findViewById(R.id.progressBar)

        presenter.attachView(this)

        btnLogin.setOnClickListener {
            hideKeyboard()
            presenter.onLoginClicked(
                etEmail.text.toString(),
                etPassword.text.toString()
            )
        }

        btnGoToRegister.setOnClickListener {
            presenter.onRegisterLinkClicked()
        }

        tvForgotPassword.setOnClickListener {
            presenter.onForgotPasswordClicked()
        }
    }

    override fun onStart() {
        super.onStart()
        presenter.checkExistingSession()
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    override fun showEmailError(message: String?) {
        tilEmail.error = message
    }

    override fun showPasswordError(message: String?) {
        tilPassword.error = message
    }

    override fun clearAllErrors() {
        tilEmail.error = null
        tilPassword.error = null
        tvLoginError.text = ""
        tvLoginError.visibility = AndroidView.GONE
    }

    override fun showGeneralError(message: String) {
        tvLoginError.text = message
        tvLoginError.visibility = AndroidView.VISIBLE
    }

    override fun showLoading() {
        progressBar.visibility = AndroidView.VISIBLE
        btnLogin.isEnabled = false
    }

    override fun hideLoading() {
        progressBar.visibility = AndroidView.GONE
        btnLogin.isEnabled = true
    }

    override fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun navigateToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    override fun navigateToForgotPassword() {
        startActivity(Intent(this, ForgotPasswordActivity::class.java))
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }
}