package com.eldroid.smartnest.ui.register

import android.content.Intent
import android.os.Bundle
import android.view.View as AndroidView
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.eldroid.smartnest.R
import com.eldroid.smartnest.ui.login.LoginActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity(), RegisterContract.View {

    private val presenter: RegisterContract.Presenter = RegisterPresenter()

    private lateinit var tilName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText

    private lateinit var btnRegister: Button
    private lateinit var btnGoToLogin: Button
    private lateinit var tvRegisterError: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        // Bind TextInputLayouts
        tilName = findViewById(R.id.tilName)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)

        // Bind EditTexts
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)

        btnRegister = findViewById(R.id.btnRegister)
        btnGoToLogin = findViewById(R.id.btnGoToLogin)
        tvRegisterError = findViewById(R.id.tvRegisterError)
        progressBar = findViewById(R.id.progressBar)

        presenter.attachView(this)

        btnRegister.setOnClickListener {
            hideKeyboard()
            presenter.onRegisterClicked(
                etName.text.toString(),
                etEmail.text.toString(),
                etPassword.text.toString(),
                etConfirmPassword.text.toString()
            )
        }

        btnGoToLogin.setOnClickListener {
            presenter.onLoginLinkClicked()
        }
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    override fun showNameError(message: String?) {
        tilName.error = message
    }

    override fun showEmailError(message: String?) {
        tilEmail.error = message
    }

    override fun showPasswordError(message: String?) {
        tilPassword.error = message
    }

    override fun showConfirmPasswordError(message: String?) {
        tilConfirmPassword.error = message
    }

    override fun clearAllErrors() {
        tilName.error = null
        tilEmail.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null
        tvRegisterError.text = ""
        tvRegisterError.visibility = AndroidView.GONE
    }

    override fun showGeneralError(message: String) {
        tvRegisterError.text = message
        tvRegisterError.visibility = AndroidView.VISIBLE
    }

    override fun showLoading() {
        progressBar.visibility = AndroidView.VISIBLE
        btnRegister.isEnabled = false
    }

    override fun hideLoading() {
        progressBar.visibility = AndroidView.GONE
        btnRegister.isEnabled = true
    }

    override fun navigateToLoginAfterCancel() {
        finish()
    }

    override fun navigateToLoginAfterSuccess() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun showRegistrationSuccess() {
        Toast.makeText(this, "Registration successful! Please log in.", Toast.LENGTH_SHORT).show()
        navigateToLoginAfterSuccess()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }
}