package doyoung.practice.healthcounternew.ble

import android.Manifest
import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*


// 블루투스 값 받아오기
interface BluetoothDataListener {
    fun onDataReceived(data: String)
}

class BluetoothLeService : Service() {

    // 블루투스 값 받아오기
    private var dataListener: BluetoothDataListener? = null
    fun setBluetoothDataListener(listener: BluetoothDataListener) {
        this.dataListener = listener
    }

    private val binder = LocalBinder()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectionState = STATE_DISCONNECTED
    lateinit var bluetoothConnectPermissionManager: BluetoothConnectPermissionManager

    companion object {
        const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        const val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
        const val ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"

        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTED = 2

        private const val TAG = "BluetoothLeService"
        private const val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"
        private const val CHARACTERISTIC_UUID = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E" // 변경된 UUID
    }

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothLeService {
            return this@BluetoothLeService
        }
    }

    override fun onCreate() {
        super.onCreate()
        bluetoothConnectPermissionManager = BluetoothConnectPermissionManager(this)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun initialize(): Boolean {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }
        return true
    }

    fun connect(address: String): Boolean {
        println("서비스 커넥트 시도!!!!")
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Bluetooth connect permission not granted")
            println("서비스 커넥트 실패!!!!")
            return false
        }

        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)
                if (bluetoothGatt == null) {
                    println("가트 서버 연결 실패!!!!")
                    Log.e(TAG, "Failed to connect to the GATT server.")
                    return false
                } else {
                    Log.i(TAG, "Successfully initiated connection to the GATT server.")
                    println("가트 서버 연결 성공!!!!")
                    return true
                }
            } catch (exception: IllegalArgumentException) {
                Log.e(TAG, "Device not found with provided address. Unable to connect.")
                return false
            }
        } ?: run {
            Log.e(TAG, "BluetoothAdapter not initialized")
            return false
        }
    }

    fun disconnect() {
        if (bluetoothConnectPermissionManager.checkBluetoothConnectPermission()) {
            bluetoothGatt?.disconnect()
        } else {
            Log.e(TAG, "Bluetooth connect permission not granted")
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (bluetoothConnectPermissionManager.checkBluetoothConnectPermission()) {
                val intentAction: String
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    intentAction = ACTION_GATT_CONNECTED
                    connectionState = STATE_CONNECTED
                    broadcastUpdate(intentAction)
                    Log.i(TAG, "Connected to GATT server.")
                    bluetoothGatt?.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    intentAction = ACTION_GATT_DISCONNECTED
                    connectionState = STATE_DISCONNECTED
                    broadcastUpdate(intentAction)
                    Log.i(TAG, "Disconnected from GATT server.")
                }
            } else {
                Log.e(TAG, "Bluetooth connect permission not granted")
            }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                val characteristic = gatt.getService(UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"))
                    ?.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID))
                characteristic?.let {
                    setCharacteristicNotification(it, true)
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicValue(characteristic)
            } else {
                Log.e(TAG, "Failed to read characteristic, status: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            handleCharacteristicValue(characteristic)
        }
    }

    private fun handleCharacteristicValue(characteristic: BluetoothGattCharacteristic) {
        val value = characteristic.value
        val data = value?.let { String(it) } ?: "null"
        Log.d(TAG, "Received data: $data")
        println("QQQQQQQ1: $data")
        // Ensure the broadcast happens on the main thread
        Handler(Looper.getMainLooper()).post {
            broadcastUpdate(ACTION_DATA_AVAILABLE, data)
        }

        // 블루투스 값 받아오기
        dataListener?.onDataReceived(data)
        println("제발!! 받은 거!!_BluetoothService: ${data}")

        broadcastUpdate(ACTION_DATA_AVAILABLE, data)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic, enabled: Boolean) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Bluetooth connect permission not granted")
            return
        }
        bluetoothGatt?.setCharacteristicNotification(characteristic, enabled)
        val descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG))
        if (descriptor != null) {
            val value = if (enabled) {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            } else {
                BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            }
            val status = bluetoothGatt?.writeDescriptor(descriptor, value)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Failed to write descriptor")
            }
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }


/*
    private fun broadcastUpdate(action: String, data: String) {
        val intent = Intent(action)
        intent.putExtra(EXTRA_DATA, data)
        sendBroadcast(intent)
    }
*/

    private fun broadcastUpdate(action: String, data: String) {
        val intent = Intent(action)
        intent.putExtra(EXTRA_DATA, data)
        Log.d(TAG, "Broadcasting update: $action with data: $data")
        sendBroadcast(intent)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun writeCharacteristic(data: String) {
        val commandCharacteristic = bluetoothGatt?.getService(UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"))
            ?.getCharacteristic(UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E"))

        if (commandCharacteristic == null) {
            Log.e(TAG, "Unable to find command characteristic")
            return
        }

        if (bluetoothConnectPermissionManager.checkBluetoothConnectPermission()) {
            val byteArray = data.toByteArray()
            Log.d(TAG, "Writing data to characteristic: $data")

            val success = bluetoothGatt?.writeCharacteristic(commandCharacteristic, byteArray, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            if (success != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Failed to write characteristic, status: $success")
            } else {
                Log.d(TAG, "Successfully wrote data to characteristic")
            }
        } else {
            Log.e(TAG, "Bluetooth connect permission not granted")
        }
    }


    /*
    // 추가된 write 메서드
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun write(data: String) {
        val commandCharacteristic = BluetoothUtils.findCommandCharacteristic(bluetoothGatt!!)

        if (commandCharacteristic == null) {
            Log.e(TAG, "Unable to find command characteristic")
            disconnect()
            return
        }

        if (bluetoothConnectPermissionManager.checkBluetoothConnectPermission()) {
            val cmdBytes = data.toByteArray()
            Log.d(TAG, "Writing data to characteristic: ${cmdBytes.joinToString()}")

            val status = bluetoothGatt?.writeCharacteristic(commandCharacteristic, cmdBytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Failed to write characteristic")
            } else {
                Log.d(TAG, "Successfully wrote data to characteristic")
            }
        } else {
            Log.e(TAG, "Bluetooth connect permission not granted")
        }
    }
     */

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun write(data: String) {
        val commandCharacteristic = BluetoothUtils.findCommandCharacteristic(bluetoothGatt!!)

        if (commandCharacteristic == null) {
            Log.e(TAG, "Unable to find command characteristic")
            disconnect()
            return
        }

        if (bluetoothConnectPermissionManager.checkBluetoothConnectPermission()) {
            // 종료 문자를 추가하여 데이터의 끝을 명확히 함 (예: '\n' 또는 '\r\n')
            val cmdBytes = (data + "\n").toByteArray(Charsets.UTF_8)
            Log.d(TAG, "Writing data to characteristic: ${cmdBytes.joinToString()}")

            // BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT 대신 다른 쓰기 유형을 시도
            val status = bluetoothGatt?.writeCharacteristic(commandCharacteristic, cmdBytes, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Failed to write characteristic")
            } else {
                Log.d(TAG, "Successfully wrote data to characteristic")
            }
        } else {
            Log.e(TAG, "Bluetooth connect permission not granted")
        }
    }
}