package id.storia.receipt.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.delay
import java.io.IOException
import java.util.*

class BluetoothPrinterManager(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    suspend fun print(deviceAddress: String, bytes: ByteArray): Result<Unit> {
        val device = adapter?.getRemoteDevice(deviceAddress) ?: return Result.failure(Exception("Device not found"))
        return try {
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            val outputStream = socket.outputStream
            
            val chunkSize = 512
            var offset = 0
            while (offset < bytes.size) {
                val length = (bytes.size - offset).coerceAtMost(chunkSize)
                outputStream.write(bytes, offset, length)
                outputStream.flush()
                offset += length
                delay(50)
            }
            delay(500)
            socket.close()
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
}
