package doyoung.practice.healthcounternew

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.bumptech.glide.Glide
import doyoung.practice.healthcounternew.databinding.ActivityCalorieBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CalorieActivity : AppCompatActivity(), FoodAdapter.ItemClickListen {
    lateinit var binding: ActivityCalorieBinding
    // ProgressBar
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var progressBarTextView: TextView

    private lateinit var totalCaloriesTextView: TextView

    lateinit var dining: String
    lateinit var diningToServer: String

    private val client = OkHttpClient()

    // 리사이클러뷰 어댑터
    private lateinit var foodAdapter: FoodAdapter

    // 카메라 이미지 관련 설정
    // 파일 불러오기
    private val getContentImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                binding.mainImg.setImageURI(uri)
                binding.mainImg.visibility = View.VISIBLE
                binding.cameraLayer.visibility = View.INVISIBLE

                if(binding.mainImg.visibility == View.VISIBLE) {
                    loadingProgressBar.visibility = View.VISIBLE
                    progressBarTextView.visibility = View.VISIBLE
                    val mainImgBitmap = (binding.mainImg.drawable as? BitmapDrawable)?.bitmap
                    mainImgBitmap?.let { bitmap ->
                        postToFoodkcal(bitmap)
                    } ?: run {
                        // 이미지를 불러오는 데 실패한 경우
                        loadingProgressBar.visibility = View.INVISIBLE
                        progressBarTextView.visibility = View.INVISIBLE
                        showToast("이미지를 불러오는 데 실패했습니다.\n칼로리 계산 버튼을 눌러 재시도 해주세요...")
                    }
                } else {
                    showToast("사진을 먼저 선택해주세요.")
                }
            } ?: showToast("이미지를 불러오는 데 실패했습니다.\n칼로리 계산 버튼을 눌러 재시도 해주세요...")
        }

    // 카메라를 실행한 후 찍은 사진을 저장
    var pictureUri: Uri? = null
    private val getTakePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            pictureUri?.let { uri ->
                // 이미지뷰에 Glide를 사용하여 이미지 설정
                Glide.with(this)
                    .load(uri)
                    .into(binding.mainImg)

                // 이미지뷰의 가시성 설정
                binding.mainImg.visibility = View.VISIBLE
                binding.cameraLayer.visibility = View.INVISIBLE

                // postToFoodkcal 호출
                loadingProgressBar.visibility = View.VISIBLE
                progressBarTextView.visibility = View.VISIBLE
                val mainImgBitmap = (binding.mainImg.drawable as? BitmapDrawable)?.bitmap
                mainImgBitmap?.let { bitmap ->
                    postToFoodkcal(bitmap)
                } ?: run {
                    // 이미지를 불러오는 데 실패한 경우
                    loadingProgressBar.visibility = View.INVISIBLE
                    progressBarTextView.visibility = View.INVISIBLE
                    showToast("이미지를 불러오는 데 실패했습니다.\n칼로리 계산 버튼을 눌러 재시도 해주세요...")
                }
            } ?: showToast("이미지를 불러오는 데 실패했습니다.\n칼로리 계산 버튼을 눌러 재시도 해주세요...")
        } else {
            showToast("사진 촬영에 실패했습니다.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalorieBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 돌아가기 버튼
        binding.goToFood.setOnClickListener {
            finish()
        }

        // 저장하기 버튼
        binding.saveButton.setOnClickListener {
            if(binding.mainImg.visibility == View.VISIBLE) {
                // FragmentFood에 띄우기 위한 Room DB의 사용
                saveDataToRoom()
                val dietInfo = foodAdapter.getDietInfo()
                val totalCalorieText = binding.totalCaloriesTextView.text.toString()
                val regex = Regex("\\d+")
                val matchResult = regex.find(totalCalorieText)
                //val totalCalorie = matchResult?.value?.toIntOrNull() ?: 0

                // 서버에 전달
                saveDataToServer2(dietInfo, diningToServer)
                println("DIETINFO: ${dietInfo}")
                showToast("저장이 완료되었습니다.")
            } else {
                showToast("저장할 정보가 없습니다.")
            }
        }

        // 상단의 식사 텍스트 변경
        dining = intent.getStringExtra("whatFood").toString()
        binding.titleTextView.text = "오늘의 ${dining}"
        if(dining == "아침") {
            diningToServer = "BREAKFAST"
        } else if(dining == "점심") {
            diningToServer = "LUNCH"
        } else {
            diningToServer = "DINNER"
        }

        // 카메라 레이어를 클릭 해, 분석할 사진 선택 카메라/갤러리
        binding.cameraLayer.setOnClickListener {
            showAlertDialog()
        }

        // ProgressBar를 두어 로딩 중 알림
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        progressBarTextView = findViewById(R.id.progressBarTextView)
        totalCaloriesTextView = findViewById(R.id.totalCaloriesTextView)

        // 계산 레이어를 클릭 해, ML 서버와 통신 후 결과 도출
        binding.calcLayer.setOnClickListener {
            if(binding.mainImg.visibility == View.VISIBLE) {
                loadingProgressBar.visibility = View.VISIBLE
                progressBarTextView.visibility = View.VISIBLE
                val mainImgBitmap = (binding.mainImg.drawable as BitmapDrawable).bitmap
                postToFoodkcal(mainImgBitmap)
            } else {
                showToast("사진을 먼저 선택해주세요.")
            }
        }

        initRecyclerView()
    }

    // 리사이클러뷰 설정
    private fun initRecyclerView() {
        val dummyList = mutableListOf<Food>(

        )

        foodAdapter = FoodAdapter(dummyList, this, binding.totalCaloriesTextView, this)
        binding.foodRecyclerView.apply {
            adapter = foodAdapter
            layoutManager = LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
            val dividerItemDecoration = DividerItemDecoration(applicationContext, LinearLayoutManager.VERTICAL)
            addItemDecoration(dividerItemDecoration)
        }
    }

    // AlertDialog를 통한 사진 촬영/로드 선택 함수
    private fun showAlertDialog() {
        val cameraBuilder = AlertDialog.Builder(this)
        cameraBuilder.setTitle("칼로리를 계산할 사진 선택")
        cameraBuilder.setMessage("사진을 어디에서 가져올까요?")

        // 루틴 설정 클릭
        cameraBuilder.setPositiveButton("촬영하기") { dialog, which ->
            showToast("카메라 어플을 켭니다..")
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                pictureUri = createImageFile()
                getTakePicture.launch(pictureUri)
            } else {
                requestPermissions(arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA)
            }
        }

        cameraBuilder.setNegativeButton("갤러리에서 가져오기") { dialog, which ->
            checkPermission()
        }
        cameraBuilder.show()
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 카메라 이미지 관련 함수
    private fun createImageFile(): Uri? {
        val now = SimpleDateFormat("yyMMdd_HHmmss").format(Date())
        val content = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "img_$now.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
        }
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, content)
    }

    companion object {
        const val REQUEST_CAMERA = 100
        const val REQUEST_IMAGE = 101
    }

    private fun checkPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 이미지 가져오기
                getContentImage.launch("image/*")
            }
            shouldShowRequestPermissionRationale(
                android.Manifest.permission.READ_MEDIA_IMAGES
            ) -> {
                showPermissionInfoDialog()
            }
            else -> {
                requestImage()
            }
        }
    }

    private fun showPermissionInfoDialog() {
        AlertDialog.Builder(this).apply {
            setMessage("이미지를 가져오기 위해, 권한이 필요합니다.")
            setNegativeButton("취소", null)
            setPositiveButton("동의") { _, _ ->
                requestImage()
            }
        }.show()
    }

    private fun requestImage() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
            REQUEST_IMAGE
        )
    }

    // postToFoodkcal 함수 정의
    private fun postToFoodkcal(mainImgBitmap: Bitmap) {
        // OkHttpClient.Builder를 사용하여 timeout 설정
        val client = OkHttpClient.Builder()
            .readTimeout(50, TimeUnit.SECONDS)
            .writeTimeout(50, TimeUnit.SECONDS)
            .build()

        // 백그라운드 스레드에서 서버와 통신
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // mainImg의 이미지를 ByteArray로 변환
                val stream = ByteArrayOutputStream()
                mainImgBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val byteArray = stream.toByteArray()

                // MultipartBody를 생성하여 이미지 파일을 추가한다.
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "up_files",
                        "image.jpg",
                        byteArray.toRequestBody(
                            "image/jpeg".toMediaTypeOrNull(),
                            0,
                            byteArray.size
                        )
                    ).build()

                // 요청 생성
                val request = Request.Builder()
                    .url("${ServerUrlManager.mlServerUrl}/analyze")
                    .addHeader("Content-Type", "multipart/form-data")
                    .post(requestBody)
                    .build()

                // 요청 보내기 전 시간 기록
                val startTime = System.currentTimeMillis()

                // 요청 보내기
                val response = client.newCall(request).execute()

                // 요청 완료 후 시간 기록
                val endTime = System.currentTimeMillis()

                // 응답 로그의 출력
                val responseBodyString = response.body?.string() ?: "Empty response"
                Log.d("MLServerResponse", responseBodyString)

                // 전체 소요 시간 출력
                val totalTime = endTime - startTime
                Log.d("MLServerResponse", "Total time: $totalTime ms")

                // 서버 응답 JSON 파싱
                val jsonObject = JSONObject(responseBodyString)

                // total_kcal 값 추출
                val totalKcal = jsonObject.getJSONObject("results").getDouble("total_kcal").toInt()

                // boxed_url 값 추출 및 IP 주소 변경
                val boxedUrl = jsonObject.getJSONObject("results").getString("boxed_url")

                // "info" 배열을 가져오기
                val infoArray = jsonObject.getJSONObject("results").getJSONArray("info")

                // Food 객체를 담을 리스트 생성
                val foodList = mutableListOf<Food>()

                // "info" 배열을 순회하며 Food 객체 생성 및 리스트에 추가
                for (i in 0 until infoArray.length()) {
                    val infoObject = infoArray.getJSONArray(i)

                    // "menu", "calorie", "kind" 추출
                    val menu = infoObject.getString(0)
                    val calorie = infoObject.getDouble(2).toInt()  // 정수부만 추출
                    val kind = dining

                    // "unclear"인 경우 해당 정보를 패스
                    if (menu.lowercase(Locale.getDefault()) == "unclear") {
                        continue
                    }

                    // Food 객체 생성 및 리스트에 추가
                    val food = Food(menu, calorie, kind)
                    foodList.add(food)
                    Thread{
                        AppDatabase.getInstance(this@CalorieActivity)?.foodDao()?.insert(food)
                    }.start()
                }

                // 통신 후 ProgressBar 감추기
                runOnUiThread {
                    // 한 끼 섭취량 kcal 표시
                    totalCaloriesTextView.visibility = View.VISIBLE
                    totalCaloriesTextView.text = "한 끼 섭취량: ${totalKcal} kcal"

                    // 분석된 데이터를 리사이클러뷰에 반영
                    foodAdapter.setData(foodList)

                    // 로딩 화면의 재설정
                    loadingProgressBar.visibility = View.INVISIBLE
                    progressBarTextView.visibility = View.INVISIBLE

                    // 박스친 이미지로의 변환
                    //val modifiedBoxedUrl = boxedUrl.replace("http://127.0.0.1:5003", "http://10.0.2.2:5003")
                    //Glide.with(this@CalorieActivity)
                    //    .load(modifiedBoxedUrl)
                    //    .into(binding.mainImg)
                }

            } catch (e: Exception) {
                runOnUiThread {
                    loadingProgressBar.visibility = View.INVISIBLE
                    progressBarTextView.visibility = View.INVISIBLE
                }
                Log.e("MLServerRequest", "Error: ${e.message}")
            }
        }
    }

    // 데이터를 룸 데이터베이스에 저장하는 함수
    private fun saveDataToRoom() {
        // FragmentFood로 이동
        finish()
    }

    override fun onClick(position: Int, cntValue: Int) {
        // TODO 온클릭 구현할 것이 있는가
    }

    private fun saveDataToServer2(dietInfo: List<Map<String, Any>>, mealTime: String) {
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val dietJson = JSONObject().apply {
            put("diet", JSONArray().apply {
                dietInfo.forEach { food ->
                    put(
                        JSONObject().apply {
                            put("dish", food["dish"])
                            put("calorie", food["calorie"])
                        }
                    )
                }
            })
        }

        val requestBody = dietJson.toString().toRequestBody(mediaType)

        // Print JSON string
        println("REQUEST-BODY: ${dietJson.toString()}")

        val request = Request.Builder()
            .url(ServerUrlManager.serverUrl + "/diet?meal-time=$mealTime")
            .addHeader("Authorization", "Bearer ${TokenManager.accessToken}")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                showToast("Failed to post workout record")
                println("CALORIE!!!: FAILED")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    showToast("Workout record posted successfully!")
                    println("CALORIE!!!: SUCCESS")
                } else {
                    showToast("Failed to post workout record: ${response.code}")
                    println("CALORIE!!!: FAILED ${response.code}")
                }
            }
        })
    }
}