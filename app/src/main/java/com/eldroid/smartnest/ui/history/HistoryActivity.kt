package com.eldroid.smartnest.ui.history

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eldroid.smartnest.R
import com.eldroid.smartnest.data.model.AlertEvent
import com.eldroid.smartnest.data.model.SensorReading
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout

class HistoryActivity : AppCompatActivity(), HistoryContract.View {

    private val presenter: HistoryContract.Presenter = HistoryPresenter()

    private val readingAdapter = HistoryReadingAdapter()
    private val alertAdapter = AlertEventAdapter()

    private lateinit var toolbar: Toolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var chipGroupRange: ChipGroup
    private lateinit var rvHistory: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: View
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        chipGroupRange = findViewById(R.id.chipGroupRange)
        rvHistory = findViewById(R.id.rvHistory)
        progressBar = findViewById(R.id.progressBar)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle)

        setupInsets()

        val macKey = intent.getStringExtra(EXTRA_MAC_KEY).orEmpty()
        val initialTab = if (intent.getStringExtra(EXTRA_TAB) == HistoryContract.Tab.ALERTS.name) {
            HistoryContract.Tab.ALERTS
        } else {
            HistoryContract.Tab.READINGS
        }

        toolbar.subtitle = intent.getStringExtra(EXTRA_DEVICE_NAME)
        toolbar.setNavigationOnClickListener { finish() }

        rvHistory.layoutManager = LinearLayoutManager(this)

        setupTabs(initialTab)
        setupRangeFilter()

        presenter.attachView(this)
        presenter.start(macKey, initialTab)
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = systemBars.top
            view.layoutParams = params
            insets
        }

        val listBasePaddingBottom = rvHistory.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(rvHistory) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = listBasePaddingBottom + systemBars.bottom)
            insets
        }
    }

    private fun setupTabs(initialTab: HistoryContract.Tab) {
        // Tab positions follow the order of HistoryContract.Tab.
        tabLayout.addTab(tabLayout.newTab().setText(R.string.history_tab_readings))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.history_tab_alerts))
        tabLayout.getTabAt(initialTab.ordinal)?.select()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                presenter.onTabSelected(HistoryContract.Tab.values()[tab.position])
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    private fun setupRangeFilter() {
        chipGroupRange.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chipRangeDay -> presenter.onRangeSelected(HistoryContract.Range.DAY)
                R.id.chipRangeWeek -> presenter.onRangeSelected(HistoryContract.Range.WEEK)
            }
        }
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    override fun showLoading() {
        progressBar.visibility = View.VISIBLE
        layoutEmpty.visibility = View.GONE
        rvHistory.visibility = View.GONE
    }

    override fun showReadings(readings: List<SensorReading>) {
        showList()
        if (rvHistory.adapter !== readingAdapter) rvHistory.adapter = readingAdapter
        readingAdapter.submitReadings(readings)
    }

    override fun showAlerts(alerts: List<AlertEvent>) {
        showList()
        if (rvHistory.adapter !== alertAdapter) rvHistory.adapter = alertAdapter
        alertAdapter.submitAlerts(alerts)
    }

    private fun showList() {
        progressBar.visibility = View.GONE
        layoutEmpty.visibility = View.GONE
        rvHistory.visibility = View.VISIBLE
    }

    override fun showEmpty(tab: HistoryContract.Tab) {
        progressBar.visibility = View.GONE
        rvHistory.visibility = View.GONE
        layoutEmpty.visibility = View.VISIBLE

        when (tab) {
            HistoryContract.Tab.READINGS -> {
                tvEmptyTitle.setText(R.string.history_empty_readings_title)
                tvEmptySubtitle.setText(R.string.history_empty_readings_subtitle)
            }

            HistoryContract.Tab.ALERTS -> {
                tvEmptyTitle.setText(R.string.history_empty_alerts_title)
                tvEmptySubtitle.setText(R.string.history_empty_alerts_subtitle)
            }
        }
    }

    override fun showError(message: String) {
        progressBar.visibility = View.GONE
        rvHistory.visibility = View.GONE
        layoutEmpty.visibility = View.VISIBLE
        tvEmptyTitle.setText(R.string.history_error_title)
        tvEmptySubtitle.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val EXTRA_MAC_KEY = "extra_mac_key"
        private const val EXTRA_DEVICE_NAME = "extra_device_name"
        private const val EXTRA_TAB = "extra_tab"

        fun intent(
            context: Context,
            macKey: String,
            deviceName: String,
            tab: HistoryContract.Tab = HistoryContract.Tab.READINGS
        ): Intent = Intent(context, HistoryActivity::class.java)
            .putExtra(EXTRA_MAC_KEY, macKey)
            .putExtra(EXTRA_DEVICE_NAME, deviceName)
            .putExtra(EXTRA_TAB, tab.name)
    }
}
