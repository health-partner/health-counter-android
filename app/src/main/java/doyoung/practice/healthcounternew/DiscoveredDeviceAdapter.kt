package doyoung.practice.healthcounternew

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import doyoung.practice.healthcounternew.ble.BluetoothScanPermissionManager
import doyoung.practice.healthcounternew.databinding.ItemBleBinding

class DiscoveredDeviceAdapter(
    var devices: MutableList<BluetoothDevice>,
    private var scanpermissionManager: BluetoothScanPermissionManager,
    private var context: Context,
    private val itemClickListener: OnItemClickListener
) : RecyclerView.Adapter<DiscoveredDeviceAdapter.DiscoveredDeviceViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(device: BluetoothDevice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscoveredDeviceViewHolder {
        val inflater =
            parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val binding = ItemBleBinding.inflate(inflater, parent, false)
        return DiscoveredDeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiscoveredDeviceViewHolder, position: Int) {
        val device = devices.get(position)

        // 권한 확인
        if (scanpermissionManager.checkBluetoothScanPermission()) {
            holder.bindName(device)
        } else {
            // 권한이 없는 경우 처리
            // scanpermissionManager.checkBluetoothScanPermission()
        }
    }

    override fun getItemCount() = devices.size

    var onItemClick: ((BluetoothDevice) -> Unit)? = null

    inner class DiscoveredDeviceViewHolder(val binding: ItemBleBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindName(device: BluetoothDevice) {
            binding.apply {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                discoveredDevice.text = device.name ?: "Unknown"
                discoveredMacAddress.text = device.address

                itemView.setOnClickListener {
                    itemClickListener.onItemClick(device)
                }
            }
        }
    }

    // 추가된 메서드
    fun setData(data: MutableList<BluetoothDevice>) {
        devices.clear()
        devices.addAll(data)
        notifyDataSetChanged()
    }
}