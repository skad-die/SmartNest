package com.eldroid.smartnest.ui.ledcontrol

interface LedControlContract {

    interface View {
        fun showConnectionStatus(isOnline: Boolean, isUnavailable: Boolean)
        fun showLastUpdate(secondsAgoText: String)
        fun showLedStatus(isOn: Boolean)
        fun showLedStatusUnknown()
        fun showCommandSent(isOn: Boolean)
        fun showSendingCommand()
        fun showCommandError(message: String)
        fun clearCommandStatus()
        fun setButtonsEnabled(enabled: Boolean)
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun onScreenOpened()
        fun onScreenClosed()
        fun onTurnOnClicked()
        fun onTurnOffClicked()
    }
}