package com.eldroid.smartnest.ui.forgotpassword

interface ForgotPasswordContract {

    interface View {
        fun showLoading()
        fun hideLoading()
        fun showEmailError(message: String?)
        fun clearAllErrors()
        fun showGeneralError(message: String)
        fun showRequestSentMessage()
        fun navigateToLogin()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun onSendResetClicked(email: String)
        fun onBackToLoginClicked()
    }
}