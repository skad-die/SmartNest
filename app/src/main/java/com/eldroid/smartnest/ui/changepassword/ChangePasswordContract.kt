package com.eldroid.smartnest.ui.changepassword

interface ChangePasswordContract {

    interface View {
        fun showError(message: String)
        fun clearError()
        fun showLoading()
        fun hideLoading()
        fun showSuccessAndFinish()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun onChangePasswordClicked(
            currentPassword: String,
            newPassword: String,
            confirmNewPassword: String
        )
    }
}