package com.eldroid.smartnest.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eldroid.smartnest.R
import com.eldroid.smartnest.data.model.SensorReading

class HistoryReadingAdapter :
    ListAdapter<HistoryReadingAdapter.Row, HistoryReadingAdapter.ViewHolder>(Diff) {

    data class Row(val reading: SensorReading, val showDayHeader: Boolean)

    private object Diff : DiffUtil.ItemCallback<Row>() {
        override fun areItemsTheSame(old: Row, new: Row) =
            old.reading.timestamp == new.reading.timestamp

        override fun areContentsTheSame(old: Row, new: Row) = old == new
    }

    /** [readings] must be ordered newest first. */
    fun submitReadings(readings: List<SensorReading>) {
        val headers = HistoryDates.startsNewDay(readings.map { it.timestamp })
        submitList(readings.mapIndexed { index, reading -> Row(reading, headers[index]) })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_reading, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDayHeader: TextView = itemView.findViewById(R.id.tvDayHeader)
        private val tvTime: TextView = itemView.findViewById(R.id.tvReadingTime)
        private val tvTemperature: TextView = itemView.findViewById(R.id.tvReadingTemperature)
        private val tvHumidity: TextView = itemView.findViewById(R.id.tvReadingHumidity)
        private val tvAirQuality: TextView = itemView.findViewById(R.id.tvReadingAirQuality)
        private val tvTray: TextView = itemView.findViewById(R.id.tvReadingTray)

        fun bind(row: Row) {
            val ctx = itemView.context
            val reading = row.reading

            tvDayHeader.visibility = if (row.showDayHeader) View.VISIBLE else View.GONE
            tvDayHeader.text = HistoryDates.day(reading.timestamp)

            tvTime.text = HistoryDates.time(reading.timestamp)
            tvTemperature.text = ctx.getString(R.string.temperature_value, reading.temperatureCelsius)
            tvHumidity.text = ctx.getString(R.string.humidity_value, reading.humidityPercent)
            tvAirQuality.text = ctx.getString(R.string.air_quality_value, reading.airQualityAdc)
            tvTray.text = reading.trayStatus
        }
    }
}
