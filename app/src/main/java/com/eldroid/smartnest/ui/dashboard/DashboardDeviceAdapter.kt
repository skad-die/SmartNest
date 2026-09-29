package com.eldroid.smartnest.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eldroid.smartnest.R
import com.eldroid.smartnest.data.model.SmartNestDevice
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class DashboardDeviceAdapter(
    private val onItemClick: (DashboardDevice) -> Unit,
    private val onUnpairClick: (SmartNestDevice) -> Unit,
    private val onChangeWifiClick: ((SmartNestDevice) -> Unit)? = null
) : ListAdapter<DashboardDevice, DashboardDeviceAdapter.ViewHolder>(Diff) {

    private object Diff : DiffUtil.ItemCallback<DashboardDevice>() {
        override fun areItemsTheSame(old: DashboardDevice, new: DashboardDevice) =
            old.device.macAddress == new.device.macAddress

        override fun areContentsTheSame(old: DashboardDevice, new: DashboardDevice) = old == new
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_connected_device, parent, false)
        return ViewHolder(view, onItemClick, onUnpairClick, onChangeWifiClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onItemClick: (DashboardDevice) -> Unit,
        private val onUnpairClick: (SmartNestDevice) -> Unit,
        private val onChangeWifiClick: ((SmartNestDevice) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivStatus: ImageView = itemView.findViewById(R.id.ivStatus)
        private val tvName: TextView = itemView.findViewById(R.id.tvDeviceName)
        private val tvSsid: TextView = itemView.findViewById(R.id.tvDeviceSsid)
        private val chipStatus: Chip = itemView.findViewById(R.id.tvDeviceStatusText)
        private val btnChangeWifi: MaterialButton = itemView.findViewById(R.id.btnChangeWifi)
        private val btnUnpair: MaterialButton = itemView.findViewById(R.id.btnUnpair)

        fun bind(item: DashboardDevice) {
            val ctx = itemView.context
            tvName.text = item.device.displayName
            tvSsid.text = ctx.getString(R.string.connected_to_network, item.device.lastSsid ?: "Unknown")

            // Bind status chip text and online icon indicator
            chipStatus.text = ctx.getString(if (item.isOnline) R.string.device_online else R.string.device_offline)

            ivStatus.setImageResource(
                if (item.isOnline) android.R.drawable.presence_online
                else android.R.drawable.presence_offline
            )

            // Optional: Toggle Change Wi-Fi availability based on online status if needed
            btnChangeWifi.isEnabled = item.isOnline

            // Handle root item click to open cage overview
            itemView.setOnClickListener {
                onItemClick(item)
            }

            // Direct button click listeners
            btnUnpair.setOnClickListener {
                onUnpairClick(item.device)
            }

            btnChangeWifi.setOnClickListener {
                onChangeWifiClick?.invoke(item.device)
            }
        }
    }
}