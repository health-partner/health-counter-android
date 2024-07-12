package doyoung.practice.healthcounternew

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import doyoung.practice.healthcounternew.ble.*
import doyoung.practice.healthcounternew.databinding.FragmentHomeBinding
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class FragmentHome : Fragment(), BluetoothLeScannerManager.ScanResultCallback{

    // 블루투스 로직의 추가
    private lateinit var bluetoothConnectPermissionManager: BluetoothConnectPermissionManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var connectpermissionManager: BluetoothConnectPermissionManager
    private lateinit var scanpermissionManager: BluetoothScanPermissionManager
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>
    private lateinit var bluetoothLeScannerManager: BluetoothLeScannerManager
    private lateinit var itemClickListener: DiscoveredDeviceAdapter.OnItemClickListener

    //private lateinit var circularProgressIndicator: CircularProgressIndicator
    //private lateinit var bluetoothDevicesRecyclerView: RecyclerView

    // discoveredDeviceAdapter로의 대체
    // private lateinit var bluetoothDeviceAdapter: BluetoothDeviceAdapter
    private lateinit var discoveredDeviceAdapter: DiscoveredDeviceAdapter


    private val scannedDevice = mutableListOf<BluetoothDevice>()

    private var bluetoothService: BluetoothLeService? = null
    private var isServiceBound = false
    private lateinit var deviceAddress: String



    lateinit var binding: FragmentHomeBinding
    lateinit var ViewPagerActivity: ViewPagerActivity
    private val client = OkHttpClient()

    private var isViewCreated = false
    private var isDataLoaded = false
    // 데일리 프로그래스 바 초기화
    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())


    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is ViewPagerActivity) ViewPagerActivity = context
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        isViewCreated = true
        loadDataIfVisible()

        return binding.root
    }

    private fun loadDataIfVisible() {
        if (isViewCreated && !isDataLoaded) {
            fetchAndUpdateProgressBar(todayDate)

            isDataLoaded = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        bluetoothConnectPermissionManager = BluetoothConnectPermissionManager(context@ViewPagerActivity)
        connectpermissionManager = BluetoothConnectPermissionManager(context@ViewPagerActivity)
        scanpermissionManager = BluetoothScanPermissionManager(context@ViewPagerActivity)


        bluetoothLeScannerManager = BluetoothLeScannerManager(
            requireContext(),
            bluetoothAdapter,
            scanpermissionManager,
            //BluetoothLeScannerManager.ScanResultCallback
            this
        )


        enableBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(context, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Bluetooth enabling request canceled or unsuccessful", Toast.LENGTH_SHORT).show()
            }
        }

        // DiscoveredDeviceAdapter의 itemClickListener 설정
        itemClickListener = object : DiscoveredDeviceAdapter.OnItemClickListener {
            override fun onItemClick(device: BluetoothDevice) {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Bluetooth 연결 권한이 없는 경우에 대한 처리
                    return
                }
                Toast.makeText(context, "${device.name}이 연결되었습니다.", Toast.LENGTH_SHORT).show()
                // TODO ViewPager액티비티 단으로 넘겨서 address, name RoutineSetting2Activity로 넘기기
                val deviceAddress = device.address
                ViewPagerActivity.setBluetoothDeviceAddress(deviceAddress)
            }
        }

        // 바인딩 로직
        // 블루투스 레이어 선언
        val bluetoothSwitch = binding.bluetoothSwitchButton
        val bluetoothLayer = binding.bluetoothLayer
        val bluetoothImage1 = binding.bluetoothOffButton
        val bluetoothImage2 = binding.bluetoothOnButton

        // 블루투스 레이어 클릭에 따른 이미지 visibility 설정
        // 초기 상태에 따른 이미지 설정 (Off)
        setBluetoothImageVisibility(bluetoothSwitch.isChecked)

        // TODO 블루투스 로직 작성
        bluetoothSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            setBluetoothImageVisibility(isChecked)
            if(isChecked) {
                // 블루투스 On
                showCustomDialog()
                connectpermissionManager.checkBluetoothConnectPermission()
                bluetoothLeScannerManager.scanLeDevice()
                enableBluetooth()

                if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                    // 블루투스가 지원되지 않거나, 활성화되지 않은 경우
                    Toast.makeText(context, "Bluetooth가 지원되지 않거나 활성화되지 않았습니다.", Toast.LENGTH_SHORT).show()
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetoothLauncher.launch(enableBtIntent)
                } else {
                    // initializeBluetoothScanner()
                }
            } else {
                // 블루투스 Off
            }
        }



        bluetoothLayer.setOnClickListener {
            // 스위치 상태 변경
            bluetoothSwitch.isChecked = !bluetoothSwitch.isChecked
        }

        // 캘린더 클릭 시, 세부 페이지로의 이동
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = "${month+1}월 ${dayOfMonth}일"
            val selectedDateToServer = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            val intent = Intent(context, CalendarDetailActivity::class.java)
            intent.putExtra("selectedDate", selectedDate)
            intent.putExtra("selectedDateToServer", selectedDateToServer)
            startActivity(intent)
        }

    }

    private fun fetchAndUpdateProgressBar(todayDate: String = "") {
        // Fetch user status
        fetchUserStatus { recommendedCalories ->
            // Update progress bar max value
            binding.dailyProgressBar.max = recommendedCalories

            // Fetch diet and workout records
            fetchDietRecord(todayDate) { dietCalories ->
                fetchWorkoutRecord(todayDate) { workoutCalories ->
                    // Calculate progress
                    val progress = dietCalories - workoutCalories

                    binding.dailyProgressBar.progress = progress
                    binding.dailyProgressBarTextView2.text = "${(progress.toFloat() / recommendedCalories * 100).toInt()}%"
                }
            }
        }
    }

    /*

        서버에서 가져오는 로직의 작성 맞을지 모르겠음

     */


    private fun fetchUserStatus(callback: (Int) -> Unit) {
        val request = Request.Builder()
            .url("${ServerUrlManager.serverUrl}/me")
            .addHeader("Authorization", "Bearer ${TokenManager.accessToken}")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                showToast("Failed to fetch user status")
            }

            override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        showToast("Failed to fetch user status: ${response.code}")
                        return
                    }

                    response.body?.string()?.let { responseBody ->
                        val json = JSONObject(responseBody)
                        val recommendedCalories = json.getInt("recommendedCalories")
                        callback(recommendedCalories)
                    }
            }
        })
    }

    private fun fetchDietRecord(date: String, callback: (Int) -> Unit) {
        val request = Request.Builder()
            .url("${ServerUrlManager.serverUrl}/diet?date=$date")
            .addHeader("Authorization", "Bearer ${TokenManager.accessToken}")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                showToast("Failed to fetch diet record")
                callback(0) // Default to 0 if request fails
            }

            override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        showToast("Failed to fetch diet record: ${response.code}")
                        callback(0) // Default to 0 if request fails
                        return
                    }

                    val contentType = response.header("Content-Type")
                    if (contentType != null && contentType.contains("application/json")) {
                        response.body?.string()?.let { responseBody ->
                            try {
                                val json = JSONObject(responseBody)
                                val totalCalorie = json.optInt("totalCalorie", 0)
                                callback(totalCalorie)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                showToast("Failed to parse diet record")
                                callback(0) // Default to 0 if parsing fails
                            }
                        } ?: run {
                            showToast("Response body is null")
                            callback(0) // Default to 0 if response body is null
                        }
                    } else {
                        showToast("Unexpected content type: $contentType")
                        callback(0) // Default to 0 if content type is not JSON
                    }
            }
        })
    }

    private fun fetchWorkoutRecord(date: String, callback: (Int) -> Unit) {
        val request = Request.Builder()
            .url("${ServerUrlManager.serverUrl}/workout/record?date=$date")
            .addHeader("Authorization", "Bearer ${TokenManager.accessToken}")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                showToast("Failed to fetch workout record")
                callback(0) // Default to 0 if request fails
            }

            override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        showToast("Failed to fetch workout record: ${response.code}")
                        callback(0) // Default to 0 if request fails
                        return
                    }

                    val contentType = response.header("Content-Type")
                    if (contentType != null && contentType.contains("application/json")) {
                        response.body?.string()?.let { responseBody ->
                            try {
                                val json = JSONObject(responseBody)
                                val totalCalorie = json.optInt("totalCalorie", 0)
                                callback(totalCalorie)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                showToast("Failed to parse workout record")
                                callback(0) // Default to 0 if parsing fails
                            }
                        } ?: run {
                            showToast("Response body is null")
                            callback(0) // Default to 0 if response body is null
                        }
                    } else {
                        showToast("Unexpected content type: $contentType")
                        callback(0) // Default to 0 if content type is not JSON
                    }
            }
        })
    }

    private fun setBluetoothImageVisibility(isBluetoothOn: Boolean) {
        if(isBluetoothOn) {
            binding.bluetoothOnButton.visibility = View.VISIBLE
            binding.bluetoothOffButton.visibility = View.INVISIBLE
        } else {
            binding.bluetoothOnButton.visibility = View.INVISIBLE
            binding.bluetoothOffButton.visibility = View.VISIBLE
        }
    }

    // 토스트 메시지 형식
    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCustomDialog() {
        // Init
        val dialogView = LayoutInflater.from(context).inflate(R.layout.ble_custom_dialog, null)
        val recyclerView: RecyclerView = dialogView.findViewById(R.id.deviceRecyclerView)

        // TODO
        //val dummyDeviceName = listOf("탐색된 기기 1", "탐색된 기기 2", "탐색된 기기 3", "탐색된 기기 4")
        //val deviceList = dummyDeviceName?.map { BluetoothDevice(it) }

        discoveredDeviceAdapter = DiscoveredDeviceAdapter(scannedDevice, scanpermissionManager, requireContext(), itemClickListener)
        discoveredDeviceAdapter.setData(scannedDevice)

        /*
        discoveredDeviceAdapter.onItemClick = { device ->
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {

            }
            Toast.makeText(context, "${device.name}이 선택되었습니다.", Toast.LENGTH_SHORT).show()
        }*/

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = discoveredDeviceAdapter

        AlertDialog.Builder(context)
            .setView(dialogView)
            .setTitle("블루투스 연결")
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    // 블루투스 관련 로직들의 추가
    private fun enableBluetooth() {
        if (bluetoothAdapter.isEnabled) {
            // Bluetooth가 이미 활성화되어 있으면 스캔을 시작
        } else {
            Toast.makeText(context, "블루투스 활성화를 하라고 알림을 띄워야함.", Toast.LENGTH_SHORT).show()
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        }
    }

    override fun onDeviceFound(device: BluetoothDevice) {
        scannedDevice.add(device)
        discoveredDeviceAdapter.notifyDataSetChanged()
    }

    override fun onScanStart() {
        //showProgressBar()
    }

    override fun onScanStop() {
        //hideProgressBar()
    }

    private fun showProgressBar() {
        //circularProgressIndicator.show()
    }

    private fun hideProgressBar() {
        //circularProgressIndicator.hide()
    }

    companion object{
        const val REQUEST_BLUETOOTH_CONNECT_PERMISSION = 100
        const val REQUEST_BLUETOOTH_SCAN_PERMISSION = 200
    }


}