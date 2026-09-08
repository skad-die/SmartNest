package com.eldroid.smartnest.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.eldroid.smartnest.R
import com.eldroid.smartnest.ui.changepassword.ChangePasswordActivity

class SettingsActivity : AppCompatActivity(), SettingsContract.View {

    private val presenter: SettingsContract.Presenter = SettingsPresenter()

    private lateinit var toolbar: Toolbar
    private lateinit var rowChangePassword: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        toolbar = findViewById(R.id.toolbar)
        rowChangePassword = findViewById(R.id.rowChangePassword)

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = view.layoutParams as android.view.ViewGroup.MarginLayoutParams
            params.topMargin = systemBars.top
            view.layoutParams = params
            insets
        }

        presenter.attachView(this)

        toolbar.setNavigationOnClickListener {
            presenter.onBackClicked()
        }

        rowChangePassword.setOnClickListener {
            presenter.onChangePasswordClicked()
        }
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    override fun navigateToChangePassword() {
        startActivity(Intent(this, ChangePasswordActivity::class.java))
    }

    override fun navigateBack() {
        finish()
    }
}