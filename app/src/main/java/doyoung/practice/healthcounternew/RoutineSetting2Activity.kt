package doyoung.practice.healthcounternew

import android.content.*
import android.os.*
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import doyoung.practice.healthcounternew.ble.BluetoothLeService
import doyoung.practice.healthcounternew.databinding.ActivityRoutineSetting2Binding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.checkDuration
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*

class RoutineSetting2Activity : AppCompatActivity() {

    //private val receivedDataList = mutableListOf<String>()

    private var bluetoothService: BluetoothLeService? = null
    private var connected = false
    private lateinit var deviceAddress: String

    private lateinit var binding: ActivityRoutineSetting2Binding

    /*
        블루투스 로직의 작성
     */

    val serviceConnection: ServiceConnection = object : ServiceConnection {
        //println("componentName: ${componentName}, service: ${service}")
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            println("서비스 초기화 시도!!!@")
            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
            println("서비스 초기화 시도!!!")
            if (!bluetoothService!!.initialize()) {
                println("서비스 초기화 실패!!!")
                Log.e(TAG, "Unable to initialize Bluetooth")
                finish()
            }
            //textView.text = deviceAddress
            bluetoothService?.connect(deviceAddress)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bluetoothService = null
        }
    }

    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    connected = true
                    updateConnectionState(R.string.connected)
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    connected = false
                    updateConnectionState(R.string.disconnected)
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    val data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA)
                    println("QQQQQQQ: ${data}")
                    displayData(data)
                }
            }
        }
    }

    // 액티비티 간 데이터 송수신
    lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private val client = OkHttpClient()

    private val selectedExercises = mutableListOf<String>() // selectedExercises를 클래스 변수로 선언

    private val exerciseIdMap = mapOf(
        "바벨 플랫 벤치 프레스" to 101,
        "바벨 인클라인 벤치 프레스" to 102,
        "스미스 벤치 프레스" to 103,
        "팩 덱 플라이" to 104,
        "바벨 숄더 프레스" to 201,
        "머신 숄더 프레스" to 202,
        "머신 레터럴 레이즈" to 203,
        "케이블 페이스 풀" to 204,
        "바벨 스쿼트" to 301,
        "스미스 스쿼트" to 302,
        "머신 레그 프레스" to 303,
        "레그 익스텐션" to 304,
        "랫 풀 다운" to 401,
        "케이블 시티드 로우" to 402,
        "스미스 바벨 로우" to 403,
        "바벨 로우" to 404
    )

    private val exerciseStringMap = mapOf(
        101 to "바벨 플랫 벤치 프레스",
        102 to "바벨 인클라인 벤치 프레스",
        103 to "스미스 벤치 프레스",
        104 to "팩 덱 플라이",
        201 to "바벨 숄더 프레스",
        202 to "머신 숄더 프레스",
        203 to "머신 레터럴 레이즈",
        204 to "케이블 페이스 풀",
        301 to "바벨 스쿼트",
        302 to "스미스 스쿼트",
        303 to "머신 레그 프레스",
        304 to "레그 익스텐션",
        401 to "랫 풀 다운",
        402 to "케이블 시티드 로우",
        403 to "스미스 바벨 로우",
        404 to "바벨 로우"
    )


    // 운동 시간 측정을 위한 설정
    private val CALORIES_PER_5_MINUTES = 37
    private var startTime: Int = 0
    private var endTime: Int = 0

    private val calendar = Calendar.getInstance()


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutineSetting2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        /*
            이전 페이지(FragmentExercise)에서 저장된 루틴을 통해 루틴을 불러오는 경우
            Intent 내부에 routineId가 있을 것임
            routineId가 있을 경우, 해당 Id를 통해
            서버의 /routines/{routineId} 엔드포인트로 GET 요청을 통해 루틴을 불러온다.\
         */


        val routineId = intent.getIntExtra("routineId", -1)
        if(routineId != -1) {
            LoadRoutineFromServer(routineId)
        }


        // Initialize Bluetooth service and set listener
        //bluetoothService?.setBluetoothDataListener(this)

        deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS) ?: ""


        binding.goToHome.setOnClickListener {
            startActivity(Intent(this, ViewPagerActivity::class.java))
        }

        // addButton 클릭 시, 운동 선택 화면으로 전환
        binding.addButton.setOnClickListener {
            val intent = Intent(this, ExerciseSelectActivity::class.java)
            resultLauncher.launch(intent)
        }

        // finishButton 클릭 시, 루틴 저장/단순 종료 선택 다이알로그 띄우기
        binding.finishButton.setOnClickListener {
            showAlertDialog()

            endTime = calendar.get(Calendar.MINUTE)
            println("종료 시간: ${endTime}")

            val durationMinutes = endTime - startTime
            val adjustedDurationMinutes = if (durationMinutes == 0) 1 else durationMinutes
            val totalCalories = (adjustedDurationMinutes / 5) * CALORIES_PER_5_MINUTES

            println("운동 시간: $durationMinutes 분, 소모 칼로리: $totalCalories")
            // 서버에 운동 시간, 소모 칼로리 포스트
            postWorkoutRecord(durationMinutes, totalCalories)

        }

        // addButton 클릭 시, 운동 선택 화면으로 전환
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode == RESULT_OK) {
                val name = result.data?.getStringExtra("exerciseName") ?: ""
                // 선택된 운동을 TextView에 표시
                showSelectedExercise(name)
            }
        }

        // 기존에 선택된 운동 리스트 받기
        //selectedExercises = intent.getStringArrayListExtra("selectedExercises") ?: arrayListOf()
        setupCancelButtons()



        // 블루투스 동작 로직
        /*
           각 StartButton에 대한 로직 작성
         */
        binding.startButton.setOnClickListener {

            startTime = calendar.get(Calendar.MINUTE)
            println("시작 시간: ${startTime}")

            bluetoothService!!.connect(deviceAddress)
            // 하드웨어에 데이터 보내기
            val data = getEditTextValues(
                binding.set1EditCount,
                binding.set2EditCount,
                binding.set3EditCount,
                binding.set4EditCount
            )
            println("보내는 데이터는!: ${data}")
            bluetoothService?.write(data)
        }

        binding.startButton2.setOnClickListener {
            bluetoothService!!.connect(deviceAddress)
            val data = getEditTextValues(
                binding.set1EditCount2,
                binding.set2EditCount2,
                binding.set3EditCount2,
                binding.set4EditCount2
            )
            bluetoothService?.write(data)
        }

        binding.startButton3.setOnClickListener {
            bluetoothService!!.connect(deviceAddress)
            val data = getEditTextValues(
                binding.set1EditCount3,
                binding.set2EditCount3,
                binding.set3EditCount3,
                binding.set4EditCount3
            )
            bluetoothService?.write(data)
        }

        binding.startButton4.setOnClickListener {
            bluetoothService!!.connect(deviceAddress)
            val data = getEditTextValues(
                binding.set1EditCount4,
                binding.set2EditCount4,
                binding.set3EditCount4,
                binding.set4EditCount4
            )
            bluetoothService?.write(data)
        }

        binding.endButton.setOnClickListener {
            sendWorkoutToServer(
                binding.set1EditCount, binding.set2EditCount, binding.set3EditCount, binding.set4EditCount,
                binding.set1EditWeight, binding.set2EditWeight, binding.set3EditWeight, binding.set4EditWeight,
                binding.exerciseNameTextView.text.toString()
            )
        }

        binding.endButton2.setOnClickListener {
            sendWorkoutToServer(binding.set1EditCount2, binding.set2EditCount2, binding.set3EditCount2, binding.set4EditCount2,
                binding.set1EditWeight2, binding.set2EditWeight2, binding.set3EditWeight2, binding.set4EditWeight2, binding.exerciseNameTextView2.text.toString())
        }

        binding.endButton3.setOnClickListener {
            sendWorkoutToServer(binding.set1EditCount3, binding.set2EditCount3, binding.set3EditCount3, binding.set4EditCount3,
                binding.set1EditWeight3, binding.set2EditWeight3, binding.set3EditWeight3, binding.set4EditWeight3, binding.exerciseNameTextView3.text.toString())
        }

        binding.endButton4.setOnClickListener {
            sendWorkoutToServer(binding.set1EditCount4, binding.set2EditCount4, binding.set3EditCount4, binding.set4EditCount4,
                binding.set1EditWeight4, binding.set2EditWeight4, binding.set3EditWeight4, binding.set4EditWeight4, binding.exerciseNameTextView4.text.toString())
        }

        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        println("gattServiceIntent 호출!!!!")
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    // 선택된 운동을 TextView에 표시하는 함수
    private fun showSelectedExercise(name: String) {
        // 운동 레이어와 텍스트뷰 배열
        val exerciseLayers = listOf(binding.exercise1Layer, binding.exercise2Layer, binding.exercise3Layer, binding.exercise4Layer)
        val exerciseNameTextViews = listOf(binding.exerciseNameTextView, binding.exerciseNameTextView2, binding.exerciseNameTextView3, binding.exerciseNameTextView4)

        // 선택된 운동을 리스트에 추가
        selectedExercises.add(name.toString())

        // 선택된 운동 종목에 따라 레이어와 텍스트뷰 설정
        selectedExercises.forEachIndexed { index, exerciseName ->
            if (index < exerciseLayers.size) {
                exerciseLayers[index].visibility = View.VISIBLE
                exerciseNameTextViews[index].text = exerciseName
            }
        }
    }

    // AlertDialog를 통한 루틴 저장/단순 종료 선택 함수
    private fun showAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("루틴을 저장하시겠습니까?")
        builder.setMessage("다음에 루틴을 쉽게 불러올 수 있습니다.")

        // 루틴 설정 클릭
        builder.setPositiveButton("네") { dialog, which ->
            showToast("루틴 저장 페이지로 이동합니다..")
            // 설정 페이지로 이동
            // 루틴 데이터를 담아 저장할 기틀을 다진다.
            val intent = Intent(this, RoutineSaveActivity::class.java)
             val routineData = createRoutineJsonAndPost()
             intent.putExtra("routineData", routineData.toString())
             intent.putStringArrayListExtra("selectedExercises", ArrayList(selectedExercises))
            startActivity(intent)
        }

        builder.setNegativeButton("아니오") { dialog, which ->
            showToast("운동을 마칩니다..")
            // 종목별 운동 기록 저장
            // createRecordPost()
            // 기존 홈페이지로 이동
            startActivity(Intent(this, ViewPagerActivity::class.java))
        }

        builder.show()
    }

    // 시간을 분 단위로 변환하여 JSON 데이터를 포스트하는 함수
    private fun postWorkoutRecord(totalTimeMinutes: Int, totalCalories: Int) {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = """
            {
                "time": $totalTimeMinutes,
                "calorie": $totalCalories
            }
        """.trimIndent().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(ServerUrlManager.serverUrl + "/workout/record")
            .addHeader("Authorization", "Bearer ${TokenManager.accessToken}")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                showToast("Failed to post workout record")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    showToast("Workout record posted successfully!")
                } else {
                    showToast("Failed to post workout record: ${response.code}")
                }
            }
        })
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 취소 버튼 클릭 리스너 설정 함수
    private fun setupCancelButtons() {
        val cancelButtons = listOf(binding.cancleButton1, binding.cancleButton2, binding.cancleButton3, binding.cancleButton4)
        cancelButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                removeExerciseAtIndex(index)
            }
        }
    }

    // 특정 인덱스의 운동을 제거하는 함수
    private fun removeExerciseAtIndex(index: Int) {
        if (index < selectedExercises.size) {
            selectedExercises.removeAt(index)
            showSelectedExercises()
        }
    }

    // 선택된 운동을 TextView에 표시하는 함수 (리스트 전체를 표시)
    private fun showSelectedExercises() {
        // 운동 레이어와 텍스트뷰 배열
        val exerciseLayers = listOf(binding.exercise1Layer, binding.exercise2Layer, binding.exercise3Layer, binding.exercise4Layer)
        val exerciseNameTextViews = listOf(binding.exerciseNameTextView, binding.exerciseNameTextView2, binding.exerciseNameTextView3, binding.exerciseNameTextView4)

        // 모든 레이어를 초기화
        exerciseLayers.forEach { it.visibility = View.GONE }

        // 선택된 운동 종목에 따라 레이어와 텍스트뷰 설정
        selectedExercises.forEachIndexed { index, exerciseName ->
            if (index < exerciseLayers.size) {
                exerciseLayers[index].visibility = View.VISIBLE
                exerciseNameTextViews[index].text = exerciseName
            }
        }
    }

    companion object {
        const val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"
        private const val TAG = "DeviceControlActivity"
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        }
    }

    private fun updateConnectionState(resourceId: Int) {
        runOnUiThread {
            //textView.setText(resourceId)
        }
    }

    private fun displayData(data: String?) {
        Log.d(TAG, "displayData called with: $data")
        if (data != null) {
            // TODO 실제, runOnUi는 endButton일 때
            runOnUiThread {
                Log.d(TAG, "Updating UI with data: $data")
                //textData.text = data
            }
        } else {
            Log.d(TAG, "Data is null")
        }
    }


    // EditText 값들을 가져와서 문자열로 변환하는 함수
    private fun getEditTextValues(vararg editTexts: EditText): String {
        return editTexts.joinToString(" ") { it.text.toString() }
    }

    /*
            운동 기록 관련

            서버에 보낼 데이터 정리 함수 밑에 쫙

     */

    /*
        운동 기록 관련 서버 전달 함수
        1. 서버에 종목별 단순 기록 전달하기
     */

    private fun sendWorkoutToServer(
        set1Count: EditText, set2Count: EditText, set3Count: EditText, set4Count: EditText,
        set1Weight: EditText, set2Weight: EditText, set3Weight: EditText, set4Weight: EditText,
        exerciseName: String
    ) {
        // 운동명으로 운동 ID 조회
        val exerciseId = exerciseIdMap[exerciseName]

        if (exerciseId == null) {
            // 예외 처리: 해당 운동명에 대한 ID가 없는 경우
            return
        }

        // 운동 정보를 담을 JSONArray 생성
        val exerciseInfoArray = JSONArray()

        // 각 세트별 EditText에서 값 추출하여 JSON 형식으로 변환
        addExerciseInfo(exerciseInfoArray, 1, set1Count.text.toString().toInt(), set1Weight.text.toString().toInt())
        addExerciseInfo(exerciseInfoArray, 2, set2Count.text.toString().toInt(), set2Weight.text.toString().toInt())
        addExerciseInfo(exerciseInfoArray, 3, set3Count.text.toString().toInt(), set3Weight.text.toString().toInt())
        addExerciseInfo(exerciseInfoArray, 4, set4Count.text.toString().toInt(), set4Weight.text.toString().toInt())

        // 최종 JSON 객체 생성
        val requestBody = JSONObject()
        requestBody.put("exerciseId", exerciseId)
        requestBody.put("exerciseInfo", exerciseInfoArray)

        // 서버로 POST 요청 보내기
        postWorkout(requestBody)
    }

    // 운동 정보를 JSON 배열에 추가하는 함수
    private fun addExerciseInfo(array: JSONArray, set: Int, repetition: Int, weight: Int) {
        val exerciseInfo = JSONObject()
        exerciseInfo.put("set", set)
        exerciseInfo.put("repetition", repetition)
        exerciseInfo.put("weight", weight)
        array.put(exerciseInfo)
    }

    // POST 요청을 보내는 함수_ 종목별 운동 기록 남기기
    private fun postWorkout(routineData: JSONObject) {
        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, routineData.toString())
        println("보내는 바디값: ${routineData.toString()}")
        val request = Request.Builder()
            .url("${ServerUrlManager.serverUrl}/workout")
            .addHeader("Authorization", "Bearer ${TokenManager.accessToken}")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread { showToast("Failed to save routine") }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        runOnUiThread { showToast("운동 종목별 기록을 서버에 보냈습니다.") }
                    } else {
                        runOnUiThread { showToast("Failed to save routine: ${response.code}") }
                    }
                }
            }
        })
    }

    /*

        넌 뭐냐


     */

    // 루틴 JSON 데이터 생성과 동시에 종목별 운동 기록 포스트 함수
    private fun createRoutineJsonAndPost(): JSONObject {
        val routineInfo = JSONArray()

        for (i in selectedExercises.indices) {
            val exerciseName = selectedExercises[i]
            val exerciseId = exerciseIdMap[exerciseName] ?: continue  // 운동 이름에 매핑된 ID가 없는 경우 건너뜀

            val exerciseObject = JSONObject()
            exerciseObject.put("exerciseId", exerciseId)

            val exerciseInfo = JSONArray()

            for (set in 1..4) {
                val setObject = JSONObject()
                val countId = resources.getIdentifier(
                    "set${set}EditCount${if (i > 0) i + 1 else ""}",
                    "id",
                    packageName
                )
                val weightId = resources.getIdentifier(
                    "set${set}EditWeight${if (i > 0) i + 1 else ""}",
                    "id",
                    packageName
                )

                val count = findViewById<EditText>(countId).text.toString().toIntOrNull() ?: 0
                val weight = findViewById<EditText>(weightId).text.toString().toIntOrNull() ?: 0

                setObject.put("set", set)
                setObject.put("repetition", count)
                setObject.put("weight", weight)
                exerciseInfo.put(setObject)
            }

            exerciseObject.put("exerciseInfo", exerciseInfo)
            routineInfo.put(exerciseObject)

            // 운동 정보를 생성하는 동시에 서버에 운동 기록을 전송
            //postExerciseRecords(exerciseId, exerciseInfo)
        }

        val routineData = JSONObject()
        routineData.put("routineTitle", "")
        routineData.put("routineInfo", routineInfo)

        return routineData
    }

    /*
           루틴 불러오기용 함수
     */
    private fun LoadRoutineFromServer(routineId: Int) {
        val request = Request.Builder()
            .url("${ServerUrlManager.serverUrl}/routines/$routineId")
            .addHeader("Authorization", "Bearer ${TokenManager.accessToken}")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                // 에러 처리
                runOnUiThread {
                    // UI에서 에러를 표시할 필요가 있으면 이곳에서 처리
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    // 응답 실패 처리
                    runOnUiThread {
                        // UI에서 응답 실패를 표시할 필요가 있으면 이곳에서 처리
                    }
                    return
                }

                response.body?.string()?.let { responseBody ->
                    try {
                        val json = JSONObject(responseBody)
                        // 서버 응답 처리
                        runOnUiThread {
                            // UI 업데이트
                            updateUIWithRoutineDetail(json)
                            println(json.toString())
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        runOnUiThread {
                            // UI에서 파싱 실패를 표시할 필요가 있으면 이곳에서 처리
                        }
                    }
                }
            }
        })
    }

    private fun updateUIWithRoutineDetail(json: JSONObject) {
        val routineTitle = json.getString("routineTitle")
        val routineInfo = json.getJSONArray("routineInfo")

        runOnUiThread {
            showToast("${routineTitle} 루틴을 불러옵니다...")
        }

        // 초기화
        binding.exercise1Layer.visibility = View.GONE
        binding.exercise2Layer.visibility = View.GONE
        binding.exercise3Layer.visibility = View.GONE
        binding.exercise4Layer.visibility = View.GONE

        for (i in 0 until routineInfo.length()) {
            val exercise = routineInfo.getJSONObject(i)
            val exerciseId = exercise.getInt("exerciseId")
            val exerciseInfo = exercise.getJSONArray("exerciseInfo")

            when (i) {
                0 -> {
                    binding.exercise1Layer.visibility = View.VISIBLE
                    updateExerciseUI(exerciseId, exerciseInfo,
                        binding.exerciseNameTextView,
                        binding.set1EditCount, binding.set2EditCount,
                        binding.set3EditCount, binding.set4EditCount,
                        binding.set1EditWeight, binding.set2EditWeight,
                        binding.set3EditWeight, binding.set4EditWeight)
                }
                1 -> {
                    binding.exercise2Layer.visibility = View.VISIBLE
                    updateExerciseUI(exerciseId, exerciseInfo,
                        binding.exerciseNameTextView2,
                        binding.set1EditCount2, binding.set2EditCount2,
                        binding.set3EditCount2, binding.set4EditCount2,
                        binding.set1EditWeight2, binding.set2EditWeight2,
                        binding.set3EditWeight2, binding.set4EditWeight2)
                }
                2 -> {
                    binding.exercise3Layer.visibility = View.VISIBLE
                    updateExerciseUI(exerciseId, exerciseInfo,
                        binding.exerciseNameTextView3,
                        binding.set1EditCount3, binding.set2EditCount3,
                        binding.set3EditCount3, binding.set4EditCount3,
                        binding.set1EditWeight3, binding.set2EditWeight3,
                        binding.set3EditWeight3, binding.set4EditWeight3)
                }
                3 -> {
                    binding.exercise4Layer.visibility = View.VISIBLE
                    updateExerciseUI(exerciseId, exerciseInfo,
                        binding.exerciseNameTextView4,
                        binding.set1EditCount4, binding.set2EditCount4,
                        binding.set3EditCount4, binding.set4EditCount4,
                        binding.set1EditWeight4, binding.set2EditWeight4,
                        binding.set3EditWeight4, binding.set4EditWeight4)
                }
            }
        }
    }

    private fun updateExerciseUI(exerciseId: Int, exerciseInfo: JSONArray,
                                 exerciseNameTextView: TextView,
                                 set1EditCount: EditText, set2EditCount: EditText,
                                 set3EditCount: EditText, set4EditCount: EditText,
                                 set1EditWeight: EditText, set2EditWeight: EditText,
                                 set3EditWeight: EditText, set4EditWeight: EditText) {
        exerciseNameTextView.text = exerciseStringMap[exerciseId] ?: "운동 이름을 찾을 수 없음"

        for (j in 0 until exerciseInfo.length()) {
            val setInfo = exerciseInfo.getJSONObject(j)
            val setCount = setInfo.getInt("repetition")
            val setWeight = setInfo.getInt("weight")

            when (j) {
                0 -> {
                    set1EditCount.setText(setCount.toString())
                    set1EditWeight.setText(setWeight.toString())
                }
                1 -> {
                    set2EditCount.setText(setCount.toString())
                    set2EditWeight.setText(setWeight.toString())
                }
                2 -> {
                    set3EditCount.setText(setCount.toString())
                    set3EditWeight.setText(setWeight.toString())
                }
                3 -> {
                    set4EditCount.setText(setCount.toString())
                    set4EditWeight.setText(setWeight.toString())
                }
            }
        }
    }

}

