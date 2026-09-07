package com.eldroid.smartnest.ui.forgotpassword

interface ForgotPasswordContract {

    interface View {
        fun showError(message: String)
        fun clearError()
        fun showLoading()
        fun hideLoading()
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