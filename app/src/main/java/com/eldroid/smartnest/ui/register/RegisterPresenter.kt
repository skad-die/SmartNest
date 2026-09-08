package com.eldroid.smartnest.ui.register

import com.eldroid.smartnest.util.AuthValidator
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterPresenter(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : RegisterContract.Presenter {

    private var view: RegisterContract.View? = null

    override fun attachView(view: RegisterContract.View) {
        this.view = view
    }

    override fun detachView() {
        view = null
    }

    override fun onRegisterClicked(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        view?.clearError()

        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        val trimmedConfirm = confirmPassword.trim()

        val nameResult = AuthValidator.validateName(trimmedName)
        if (nameResult is AuthValidator.ValidationResult.Invalid) {
            view?.showError(nameResult.message)
            return
        }

        val emailResult = AuthValidator.validateEmail(trimmedEmail)
        if (emailResult is AuthValidator.ValidationResult.Invalid) {
            view?.showError(emailResult.message)
            return
        }

        val passwordResult = AuthValidator.validatePasswordComplexity(trimmedPassword)
        if (passwordResult is AuthValidator.ValidationResult.Invalid) {
            view?.showError(passwordResult.message)
            return
        }

        val matchResult = AuthValidator.validatePasswordMatch(trimmedPassword, trimmedConfirm)
        if (matchResult is AuthValidator.ValidationResult.Invalid) {
            view?.showError(matchResult.message)
            return
        }

        view?.showLoading()

        auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val profileUpdate = UserProfileChangeRequest.Builder()
                        .setDisplayName(trimmedName)
                        .build()
                    auth.currentUser?.updateProfile(profileUpdate)
                        ?.addOnCompleteListener {
                            view?.hideLoading()
                            auth.signOut()
                            view?.showRegistrationSuccess()
                        }
                } else {
                    view?.hideLoading()
                    val message = when (task.exception) {
                        is FirebaseAuthUserCollisionException -> "An account with this email already exists."
                        is FirebaseAuthWeakPasswordException -> "Password is too weak."
                        is FirebaseNetworkException -> "Network error. Check your connection and try again."
                        else -> "Unable to create account right now. Please try again."
                    }
                    view?.showRegistrationError(message)
                }
            }
    }

    override fun onLoginLinkClicked() {
        view?.navigateToLoginAfterCancel()
    }
}