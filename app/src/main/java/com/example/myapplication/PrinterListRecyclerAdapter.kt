package com.example.myapplication

import android.content.Context
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView

import com.brother.sdk.lmprinter.Channel
import com.example.myapplication.databinding.ItemPrinterListBinding
import kotlinx.parcelize.Parcelize

class PrinterListRecyclerAdapter(
    val context: Context,
    private var data: List<IPrinterInfo>,
    private val onItemSelected: (IPrinterInfo) -> Unit
) : RecyclerView.Adapter<PrinterListRecyclerAdapter.PrinterListRecyclerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrinterListRecyclerViewHolder {
        val binding: ItemPrinterListBinding =
            DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.item_printer_list, parent, false)

        return PrinterListRecyclerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PrinterListRecyclerViewHolder, position: Int) {
        val info = data.getOrNull(position) ?: return
        holder.binding.printerItemRoot.setOnClickListener {
            onItemSelected.invoke(info)
        }
        holder.binding.printerItemMain.text = info.modelName
        if (info is WiFiPrinterInfo) {
            holder.binding.printerItemSub.text = info.ipv4Address
        }

//        if (info is BluetoothPrinterInfo) {
//            holder.binding.printerItemSub.text = info.macAddress
//        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class PrinterListRecyclerViewHolder(var binding: ItemPrinterListBinding) : RecyclerView.ViewHolder(binding.root)
}

@Parcelize
class WiFiPrinterInfo(
    override var modelName: String,
    override var ipv4Address: String,
    override var channelType: Channel.ChannelType = Channel.ChannelType.Wifi
) : IPrinterInfo, Parcelable
