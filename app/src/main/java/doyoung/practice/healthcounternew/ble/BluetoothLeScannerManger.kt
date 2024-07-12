package doyoung.practice.healthcounternew.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import java.util.*

class BluetoothLeScannerManager(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter,
    private val scanpermissionManager: BluetoothScanPermissionManager,
    private val callback: ScanResultCallback
) {
    interface ScanResultCallback {
        fun onDeviceFound(device: BluetoothDevice)
        fun onScanStart()
        fun onScanStop()
    }

    private val bluetoothLeScanner: BluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    private val handler = Handler(Looper.getMainLooper())
    private val SCAN_PERIOD: Long = 10000 // 10초 동안 스캔
    private var scanning = false

    // 디바이스 중복 검사를 위한 디바이스
    private val scannedDeviceAddresses = mutableSetOf<String>()

    fun scanLeDevice() {
        if (scanpermissionManager.checkBluetoothScanPermission()) {
            if (!scanning) {
                callback.onScanStart()
                val scanFilter = ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"))) // 특정 서비스 UUID로 필터링
                    .build()
                val scanFilters = listOf(scanFilter)

                val scanSettings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // 스캔 모드 설정
                    .build()

                handler.postDelayed({
                    stopScan()
                }, SCAN_PERIOD)

                scanning = true
                bluetoothLeScanner.startScan(scanFilters, scanSettings, leScanCallback)
                Toast.makeText(context, "BLE 기기 스캔 시작", Toast.LENGTH_SHORT).show()
            } else {
                stopScan()
            }
        }
    }

    private fun stopScan() {
        scanning = false
        scanpermissionManager.checkBluetoothScanPermission()
        bluetoothLeScanner.stopScan(leScanCallback)
        callback.onScanStop()
        Toast.makeText(context, "BLE 기기 스캔 중지", Toast.LENGTH_SHORT).show()
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            scanpermissionManager.checkBluetoothScanPermission()
            super.onScanResult(callbackType, result)
            val deviceAddress = result.device.address
            Log.e("BluetoothScan", "Device found: ${result.device.name} - ${result.device.address}")
            if (!scannedDeviceAddresses.contains(deviceAddress)) {
                scannedDeviceAddresses.add(deviceAddress)
                callback.onDeviceFound(result.device)
            }
        }
    }
}