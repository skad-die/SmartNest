package com.eldroid.smartnest.ui.register

interface RegisterContract {

    interface View {
        fun showError(message: String)
        fun clearError()
        fun showLoading()
        fun hideLoading()
        fun navigateToLoginAfterCancel()
        fun navigateToLoginAfterSuccess()
        fun showRegistrationError(message: String)
        fun showRegistrationSuccess()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun onRegisterClicked(name: String, email: String, password: String, confirmPassword: String)
        fun onLoginLinkClicked()
    }
}