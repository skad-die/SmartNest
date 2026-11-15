package com.eldroid.smartnest.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eldroid.smartnest.R
import com.eldroid.smartnest.alerts.AlertFormatter
import com.eldroid.smartnest.data.model.AlertEvent

class AlertEventAdapter :
    ListAdapter<AlertEventAdapter.Row, AlertEventAdapter.ViewHolder>(Diff) {

    data class Row(val alert: AlertEvent, val showDayHeader: Boolean)

    private object Diff : DiffUtil.ItemCallback<Row>() {
        override fun areItemsTheSame(old: Row, new: Row) =
            old.alert.timestamp == new.alert.timestamp && old.alert.type == new.alert.type

        override fun areContentsTheSame(old: Row, new: Row) = old == new
    }

    /** [alerts] must be ordered newest first. */
    fun submitAlerts(alerts: List<AlertEvent>) {
        val headers = HistoryDates.startsNewDay(alerts.map { it.timestamp })
        submitList(alerts.mapIndexed { index, alert -> Row(alert, headers[index]) })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert_event, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDayHeader: TextView = itemView.findViewById(R.id.tvDayHeader)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivAlertIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvAlertTitle)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvAlertMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvAlertTime)

        fun bind(row: Row) {
            val ctx = itemView.context
            val alert = row.alert

            tvDayHeader.visibility = if (row.showDayHeader) View.VISIBLE else View.GONE
            tvDayHeader.text = HistoryDates.day(alert.timestamp)

            ivIcon.setImageResource(AlertFormatter.icon(alert))
            tvTitle.text = AlertFormatter.title(ctx, alert)
            tvTitle.setTextColor(ContextCompat.getColor(ctx, AlertFormatter.color(alert)))
            tvMessage.text = AlertFormatter.message(ctx, alert)
            tvTime.text = HistoryDates.time(alert.timestamp)
        }
    }
}
