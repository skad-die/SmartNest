package com.eldroid.smartnest.ui.changepassword

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
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.eldroid.smartnest.R

class ChangePasswordActivity : AppCompatActivity(), ChangePasswordContract.View {

    private val presenter: ChangePasswordContract.Presenter = ChangePasswordPresenter()

    private lateinit var toolbar: Toolbar
    private lateinit var etCurrentPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmNewPassword: EditText
    private lateinit var btnChangePassword: Button
    private lateinit var tvChangePasswordError: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_password)

        toolbar = findViewById(R.id.toolbar)
        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        tvChangePasswordError = findViewById(R.id.tvChangePasswordError)
        progressBar = findViewById(R.id.progressBar)

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = view.layoutParams as android.view.ViewGroup.MarginLayoutParams
            params.topMargin = systemBars.top
            view.layoutParams = params
            insets
        }

        presenter.attachView(this)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        btnChangePassword.setOnClickListener {
            hideKeyboard()
            presenter.onChangePasswordClicked(
                etCurrentPassword.text.toString(),
                etNewPassword.text.toString(),
                etConfirmNewPassword.text.toString()
            )
        }
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    override fun showError(message: String) {
        tvChangePasswordError.text = message
    }

    override fun clearError() {
        tvChangePasswordError.text = ""
    }

    override fun showLoading() {
        progressBar.visibility = AndroidView.VISIBLE
        btnChangePassword.isEnabled = false
    }

    override fun hideLoading() {
        progressBar.visibility = AndroidView.GONE
        btnChangePassword.isEnabled = true
    }

    override fun showSuccessAndFinish() {
        Toast.makeText(this, "Password updated successfully.", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }
}