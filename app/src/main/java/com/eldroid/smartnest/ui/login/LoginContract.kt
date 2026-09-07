package com.eldroid.smartnest.ui.login

interface LoginContract {

    interface View {
        fun showError(message: String)
        fun clearError()
        fun showLoading()
        fun hideLoading()
        fun navigateToDashboard()
        fun navigateToRegister()
        fun navigateToForgotPassword()
        fun showAuthError(message: String)
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