package com.eldroid.smartnest.ui.register

interface RegisterContract {

    interface View {
        fun showNameError(message: String?)
        fun showEmailError(message: String?)
        fun showPasswordError(message: String?)
        fun showConfirmPasswordError(message: String?)
        fun clearAllErrors()

        fun showLoading()
        fun hideLoading()
        fun navigateToLoginAfterCancel()
        fun navigateToLoginAfterSuccess()
        fun showGeneralError(message: String)
        fun showRegistrationSuccess()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun onRegisterClicked(name: String, email: String, password: String, confirmPassword: String)
        fun onLoginLinkClicked()
    }
}