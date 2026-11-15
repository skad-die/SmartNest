package com.eldroid.smartnest.ui.setupdevice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.eldroid.smartnest.R
import com.eldroid.smartnest.data.model.WifiNetwork

class WifiNetworkAdapter(
    private val onNetworkClicked: (WifiNetwork) -> Unit
) : RecyclerView.Adapter<WifiNetworkAdapter.ViewHolder>() {

    private val networks = mutableListOf<WifiNetwork>()

    fun submitList(newNetworks: List<WifiNetwork>) {
        val oldItems = networks.toList()
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = oldItems.size
            override fun getNewListSize() = newNetworks.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                oldItems[oldPos].ssid == newNetworks[newPos].ssid
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                oldItems[oldPos] == newNetworks[newPos]
        })
        networks.clear()
        networks.addAll(newNetworks)
        diff.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wifi_network, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(networks[position], onNetworkClicked)
    }

    override fun getItemCount(): Int = networks.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSsid: TextView = itemView.findViewById(R.id.tvSsid)
        private val ivLockIcon: ImageView = itemView.findViewById(R.id.ivLockIcon)

        fun bind(network: WifiNetwork, onNetworkClicked: (WifiNetwork) -> Unit) {
            tvSsid.text = network.ssid
            ivLockIcon.visibility = if (network.secure) View.VISIBLE else View.INVISIBLE
            itemView.setOnClickListener { onNetworkClicked(network) }
        }
    }
}