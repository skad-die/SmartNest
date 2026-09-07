package com.eldroid.smartnest.ui.register

import android.content.Intent
import android.os.Bundle
import android.view.View as AndroidView
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.eldroid.smartnest.R
import com.eldroid.smartnest.ui.login.LoginActivity

class RegisterActivity : AppCompatActivity(), RegisterContract.View {

    private val presenter: RegisterContract.Presenter = RegisterPresenter()

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnGoToLogin: Button
    private lateinit var tvRegisterError: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

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

    override fun showError(message: String) {
        tvRegisterError.text = message
    }

    override fun clearError() {
        tvRegisterError.text = ""
    }

    override fun showLoading() {
        progressBar.visibility = AndroidView.VISIBLE
        btnRegister.isEnabled = false
    }

    override fun hideLoading() {
        progressBar.visibility = AndroidView.GONE
        btnRegister.isEnabled = true
    }

    override fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun showRegistrationError(message: String) {
        tvRegisterError.text = message
    }

    override fun showRegistrationSuccess() {
        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
        navigateToLogin()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }
}