package com.eldroid.smartnest.ui.changepassword

import com.eldroid.smartnest.util.AuthValidator
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

class ChangePasswordPresenter(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ChangePasswordContract.Presenter {

    private var view: ChangePasswordContract.View? = null

    override fun attachView(view: ChangePasswordContract.View) {
        this.view = view
    }

    override fun detachView() {
        view = null
    }

    override fun onChangePasswordClicked(
        currentPassword: String,
        newPassword: String,
        confirmNewPassword: String
    ) {
        view?.clearError()

        val trimmedCurrent = currentPassword.trim()
        val trimmedNew = newPassword.trim()
        val trimmedConfirm = confirmNewPassword.trim()

        val currentResult = AuthValidator.validateLoginPassword(trimmedCurrent)
        if (currentResult is AuthValidator.ValidationResult.Invalid) {
            view?.showError(currentResult.message)
            return
        }

        val newPasswordResult = AuthValidator.validatePasswordComplexity(trimmedNew)
        if (newPasswordResult is AuthValidator.ValidationResult.Invalid) {
            view?.showError(newPasswordResult.message)
            return
        }

        val matchResult = AuthValidator.validatePasswordMatch(trimmedNew, trimmedConfirm)
        if (matchResult is AuthValidator.ValidationResult.Invalid) {
            view?.showError(matchResult.message)
            return
        }

        if (trimmedCurrent == trimmedNew) {
            view?.showError("New password must be different from your current password.")
            return
        }

        val user = auth.currentUser
        val email = user?.email
        if (user == null || email == null) {
            view?.showError("Unable to verify your account. Please log in again.")
            return
        }

        view?.showLoading()

        val credential = EmailAuthProvider.getCredential(email, trimmedCurrent)
        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (!reauthTask.isSuccessful) {
                    view?.hideLoading()
                    val message = when (reauthTask.exception) {
                        is FirebaseAuthInvalidCredentialsException -> "Current password is incorrect."
                        is FirebaseNetworkException -> "Network error. Check your connection and try again."
                        else -> "Unable to verify current password. Please try again."
                    }
                    view?.showError(message)
                    return@addOnCompleteListener
                }

                user.updatePassword(trimmedNew)
                    .addOnCompleteListener { updateTask ->
                        view?.hideLoading()
                        if (updateTask.isSuccessful) {
                            view?.showSuccessAndFinish()
                        } else {
                            val message = when (updateTask.exception) {
                                is FirebaseAuthWeakPasswordException -> "Password is too weak."
                                is FirebaseNetworkException -> "Network error. Check your connection and try again."
                                else -> "Unable to update password right now. Please try again."
                            }
                            view?.showError(message)
                        }
                    }
            }
    }
}