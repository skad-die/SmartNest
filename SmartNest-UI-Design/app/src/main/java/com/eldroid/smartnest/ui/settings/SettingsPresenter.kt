package com.eldroid.smartnest.ui.settings

class SettingsPresenter : SettingsContract.Presenter {

    private var view: SettingsContract.View? = null

    override fun attachView(view: SettingsContract.View) {
        this.view = view
    }

    override fun detachView() {
        view = null
    }

    override fun onChangePasswordClicked() {
        view?.navigateToChangePassword()
    }

    override fun onBackClicked() {
        view?.navigateBack()
    }
}