package com.example.myapplication

import android.content.Context
import androidx.lifecycle.ViewModel

import com.brother.sdk.lmprinter.Channel

class PrinterInterfaceViewModel : ViewModel() {
    fun getPrinterTypeList(context: Context): List<String> {
        return Channel.ChannelType.values().map { context.getString(it.getResId()) }.sorted()
    }
}
fun Channel.ChannelType.getResId(): Int {
    return when (this) {
        Channel.ChannelType.USB -> R.string.usb
        Channel.ChannelType.Wifi -> R.string.network
        Channel.ChannelType.Bluetooth -> R.string.classic_bluetooth
        Channel.ChannelType.BluetoothLowEnergy -> R.string.bluetooth_low_energy
    }
}