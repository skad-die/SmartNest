package com.eldroid.smartnest.ui.setupdevice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.eldroid.smartnest.R
import com.eldroid.smartnest.data.model.SmartNestDevice
import com.google.android.material.button.MaterialButton

class ConnectedDeviceAdapter(
    private val onChangeWifiClicked: (SmartNestDevice) -> Unit,
    private val onUnpairClicked: (SmartNestDevice) -> Unit
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
        holder.bind(devices[position], onChangeWifiClicked, onUnpairClicked)
    }

    override fun getItemCount(): Int = devices.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDeviceName: TextView = itemView.findViewById(R.id.tvDeviceName)
        private val tvDeviceSsid: TextView = itemView.findViewById(R.id.tvDeviceSsid)
        private val btnChangeWifi: MaterialButton = itemView.findViewById(R.id.btnChangeWifi)
        private val btnUnpair: MaterialButton = itemView.findViewById(R.id.btnUnpair)

        fun bind(
            device: SmartNestDevice,
            onChangeWifi: (SmartNestDevice) -> Unit,
            onUnpair: (SmartNestDevice) -> Unit
        ) {
            tvDeviceName.text = device.displayName
            tvDeviceSsid.text = itemView.context.getString(
                R.string.connected_to_network,
                device.lastSsid
            )
            btnChangeWifi.setOnClickListener { onChangeWifi(device) }
            btnUnpair.setOnClickListener {
                AlertDialog.Builder(itemView.context)
                    .setTitle(R.string.unpair_title)
                    .setMessage(itemView.context.getString(R.string.unpair_message, device.displayName))
                    .setPositiveButton(R.string.unpair) { _, _ -> onUnpair(device) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
    }
}