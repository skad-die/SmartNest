package com.eldroid.smartnest.ui.settings

interface SettingsContract {

    interface View {
        fun navigateToChangePassword()
        fun navigateBack()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun onChangePasswordClicked()
        fun onBackClicked()
    }
}