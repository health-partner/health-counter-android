package doyoung.practice.healthcounternew

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import doyoung.practice.healthcounternew.databinding.ActivityRoutineSaveBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class RoutineSaveActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoutineSaveBinding
    private lateinit var loadedExerciseAdapter: LoadedExerciseAdapter
    private lateinit var nameToServer: String
    private lateinit var routineData: JSONObject

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutineSaveBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initRecyclerView()

        // 인텐트를 통해 전달된 JSON 데이터를 수신
        val routineDataString = intent.getStringExtra("routineData")
        routineData = JSONObject(routineDataString ?: "{}")


        binding.goToSetting.setOnClickListener {
            finish()
        }

        binding.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 변경 전
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 변경 중
                val text = s?.toString() // Editable을 String으로 변환
                nameToServer = text ?: ""
                // 만질게 없는 듯 보임
            }

            override fun afterTextChanged(s: Editable?) {
                // 변경 후
            }
        })

        binding.saveButton.setOnClickListener {
            val routineTitle = binding.editText.text.toString()
            routineData.put("routineTitle", routineTitle)

            // 루틴 서버에 포스트
            postRoutine(routineData)

            startActivity(Intent(this, ViewPagerActivity::class.java))
        }

        // Intent에서 운동 리스트를 추출하여 리사이클러뷰 초기화에 영향
        val exerciseNames = intent.getStringArrayListExtra("selectedExercises")
        val exerciseList = exerciseNames?.map { Exercise(it) }
        loadedExerciseAdapter.setData(exerciseList)

    }

    private fun initRecyclerView() {
        loadedExerciseAdapter = LoadedExerciseAdapter(mutableListOf())
        binding.loadedExerciseRecyclerView.apply {
            adapter = loadedExerciseAdapter
            layoutManager =
                LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
            val dividerItemDecoration =
                DividerItemDecoration(applicationContext, LinearLayoutManager.VERTICAL)
            addItemDecoration(dividerItemDecoration)
        }
    }

    // 토스트 메시지 형식
    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun postRoutine(routineData: JSONObject) {
        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, routineData.toString())
        val request = Request.Builder()
            .url("${ServerUrlManager.serverUrl}/routines")
            .addHeader("Authorization", "Bearer ${TokenManager.accessToken}")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread { showToast("Failed to save routine") }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread { showToast("Failed to save routine: ${response.code}") }
                    return
                }

                response.body?.string()?.let { responseBody ->
                    println("출력되는 내용: $responseBody")
                }

                runOnUiThread {
                    showToast("루틴을 저장합니다..")
                }
            }
        })
    }
}