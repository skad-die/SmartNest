package com.eldroid.smartnest.ui.login

import com.eldroid.smartnest.util.AuthValidator
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginPresenter(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : LoginContract.Presenter {

    private var view: LoginContract.View? = null

    override fun attachView(view: LoginContract.View) {
        this.view = view
    }

    override fun detachView() {
        view = null
    }

    override fun checkExistingSession() {
        if (auth.currentUser != null) {
            view?.navigateToDashboard()
        }
    }

    override fun onLoginClicked(email: String, password: String) {
        view?.clearError()

        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        val emailResult = AuthValidator.validateEmail(trimmedEmail)
        if (emailResult is AuthValidator.ValidationResult.Invalid) {
            view?.showError(emailResult.message)
            return
        }

        val passwordResult = AuthValidator.validateLoginPassword(trimmedPassword)
        if (passwordResult is AuthValidator.ValidationResult.Invalid) {
            view?.showError(passwordResult.message)
            return
        }

        view?.showLoading()

        auth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword)
            .addOnCompleteListener { task ->
                view?.hideLoading()
                if (task.isSuccessful) {
                    view?.navigateToDashboard()
                } else {
                    val message = when (task.exception) {
                        is FirebaseAuthInvalidUserException,
                        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
                        is FirebaseNetworkException -> "Network error. Check your connection and try again."
                        else -> "Unable to log in right now. Please try again."
                    }
                    view?.showAuthError(message)
                }
            }
    }

    override fun onRegisterLinkClicked() {
        view?.navigateToRegister()
    }

    override fun onForgotPasswordClicked() {
        view?.navigateToForgotPassword()
    }
}