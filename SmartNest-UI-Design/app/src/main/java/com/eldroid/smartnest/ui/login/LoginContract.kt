package com.eldroid.smartnest.ui.login

interface LoginContract {

    interface View {
        fun showEmailError(message: String?)
        fun showPasswordError(message: String?)
        fun clearAllErrors()
        fun showGeneralError(message: String)
        fun showLoading()
        fun hideLoading()
        fun navigateToDashboard()
        fun navigateToRegister()
        fun navigateToForgotPassword()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun onLoginClicked(email: String, password: String)
        fun onRegisterLinkClicked()
        fun onForgotPasswordClicked()
        fun checkExistingSession()
    }
}