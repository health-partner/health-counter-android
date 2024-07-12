package doyoung.practice.healthcounternew.ble

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import doyoung.practice.healthcounternew.FragmentHome
import doyoung.practice.healthcounternew.ViewPagerActivity

// 블루투스 관련 권한 로직 처리 매니저


class BluetoothConnectPermissionManager(private val context: Context) {
    fun checkBluetoothConnectPermission(): Boolean {
        return when {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED -> {
                true
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                context as Activity,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) -> {
                showPermissionInfoDialog()
                false
            }
            else -> {
                requestBluetoothConnectPermission()
                false
            }
        }
    }

    private fun showPermissionInfoDialog() {
        AlertDialog.Builder(context).apply {
            setMessage("Bluetooth 연결을 위해 BLUETOOTH_CONNECT 권한이 필요합니다.")
            setNegativeButton("취소", null)
            setPositiveButton("동의") { _, _ ->
                requestBluetoothConnectPermission()
            }
        }.show()
    }

    private fun requestBluetoothConnectPermission() {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT),
            FragmentHome.REQUEST_BLUETOOTH_CONNECT_PERMISSION
        )
    }
}