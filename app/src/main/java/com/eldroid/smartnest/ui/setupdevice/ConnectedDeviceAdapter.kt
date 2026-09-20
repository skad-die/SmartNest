package com.eldroid.smartnest.ui.setupdevice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.eldroid.smartnest.R
import com.eldroid.smartnest.data.model.SmartNestDevice

class ConnectedDeviceAdapter(
    private val onChangeWifiClicked: (SmartNestDevice) -> Unit
) : RecyclerView.Adapter<ConnectedDeviceAdapter.ViewHolder>() {

    private val devices = mutableListOf<SmartNestDevice>()

    fun submitList(newDevices: List<SmartNestDevice>) {
        devices.clear()
        devices.addAll(newDevices)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_connected_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(devices[position], onChangeWifiClicked)
    }

    override fun getItemCount(): Int = devices.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDeviceName: TextView = itemView.findViewById(R.id.tvDeviceName)
        private val tvDeviceSsid: TextView = itemView.findViewById(R.id.tvDeviceSsid)
        private val btnChangeWifi: com.google.android.material.button.MaterialButton =
            itemView.findViewById(R.id.btnChangeWifi)

        fun bind(device: SmartNestDevice, onChangeWifiClicked: (SmartNestDevice) -> Unit) {
            tvDeviceName.text = device.displayName
            tvDeviceSsid.text = itemView.context.getString(
                R.string.connected_to_network,
                device.lastSsid
            )
            btnChangeWifi.setOnClickListener { onChangeWifiClicked(device) }
        }
    }
}