package com.eldroid.smartnest.ui.forgotpassword

import com.eldroid.smartnest.util.AuthValidator
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class ForgotPasswordPresenter(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ForgotPasswordContract.Presenter {

    private var view: ForgotPasswordContract.View? = null

    override fun attachView(view: ForgotPasswordContract.View) {
        this.view = view
    }

    override fun detachView() {
        view = null
    }

    override fun onSendResetClicked(email: String) {
        view?.clearError()

        val trimmedEmail = email.trim()

        val emailResult = AuthValidator.validateEmail(trimmedEmail)
        if (emailResult is AuthValidator.ValidationResult.Invalid) {
            view?.showError(emailResult.message)
            return
        }

        view?.showLoading()

        auth.sendPasswordResetEmail(trimmedEmail)
            .addOnCompleteListener { task ->
                view?.hideLoading()
                if (task.isSuccessful) {
                    view?.showRequestSentMessage()
                } else {
                    when (task.exception) {
                        is FirebaseNetworkException ->
                            view?.showError("Network error. Check your connection and try again.")
                        is FirebaseTooManyRequestsException ->
                            view?.showError("Too many attempts. Please wait a moment and try again.")
                        is FirebaseAuthInvalidUserException -> {
                            view?.showRequestSentMessage()
                        }
                        else -> {
                            val errorMsg = task.exception?.localizedMessage ?: "Failed to send reset email. Please try again."
                            view?.showError(errorMsg)
                        }
                    }
                }
            }
    }

    override fun onBackToLoginClicked() {
        view?.navigateToLogin()
    }
}