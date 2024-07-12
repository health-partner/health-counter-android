package doyoung.practice.healthcounternew.ble

import android.app.AlertDialog
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import doyoung.practice.healthcounternew.FragmentHome
import doyoung.practice.healthcounternew.ViewPagerActivity


class BluetoothScanPermissionManager(private val activity: ViewPagerActivity) {
    fun checkBluetoothScanPermission(): Boolean {
        return when {
            ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED -> {
                true
            }
            activity.shouldShowRequestPermissionRationale(
                android.Manifest.permission.BLUETOOTH_SCAN
            ) -> {
                showPermissionInfoDialog()
                false
            }
            else -> {
                requestBluetoothScanPermission()
                false
            }
        }
    }

    private fun showPermissionInfoDialog() {
        AlertDialog.Builder(activity).apply {
            setMessage("Bluetooth 스캔을 위해 BLUETOOTH_SCAN 권한이 필요합니다.")
            setNegativeButton("취소", null)
            setPositiveButton("동의") { _, _ ->
                requestBluetoothScanPermission()
            }
        }.show()
    }

    private fun requestBluetoothScanPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(android.Manifest.permission.BLUETOOTH_SCAN),
            FragmentHome.REQUEST_BLUETOOTH_SCAN_PERMISSION
        )
    }
}