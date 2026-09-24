package com.eldroid.smartnest.ui.forgotpassword

interface ForgotPasswordContract {

    interface View {
        fun showLoading()
        fun hideLoading()
        fun showError(message: String)
        fun clearError()
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