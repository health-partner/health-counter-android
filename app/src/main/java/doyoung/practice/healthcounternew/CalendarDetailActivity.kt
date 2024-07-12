package doyoung.practice.healthcounternew

import android.R
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import doyoung.practice.healthcounternew.databinding.ActivityCalendarDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class CalendarDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 상단의 날짜 변경
        val selectedDate = intent.getStringExtra("selectedDate")
        binding.dateTextView.text = selectedDate
        val selectedDateToServer = intent.getStringExtra("selectedDateToServer")

        // 상단의 뒤로가기 버튼을 누를 경우, 이전 페이지로 되돌아가기
        binding.goToHome.setOnClickListener {
            finish()
        }

        // 드롭다운 <운동 일지>
        binding.dropDownLayer.setOnClickListener {
            if(binding.downArrowButton.visibility == View.INVISIBLE) {
                binding.rightArrowButton.visibility = View.INVISIBLE
                binding.downArrowButton.visibility = View.VISIBLE
                binding.exerciseLogLayer.visibility = View.VISIBLE
                fetchWorkoutRecord(selectedDateToServer!!)
            } else {
                binding.rightArrowButton.visibility = View.VISIBLE
                binding.downArrowButton.visibility = View.INVISIBLE
                binding.exerciseLogLayer.visibility = View.GONE
            }
        }

        // 드롭다운 <식단 일지>
        binding.dropDownLayer2.setOnClickListener {
            if(binding.downArrowButton2.visibility == View.INVISIBLE) {
                binding.rightArrowButton2.visibility = View.INVISIBLE
                binding.downArrowButton2.visibility = View.VISIBLE
                binding.foodLogLayer.visibility = View.VISIBLE
                fetchDietRecord(selectedDateToServer!!)
            } else {
                binding.rightArrowButton2.visibility = View.VISIBLE
                binding.downArrowButton2.visibility = View.INVISIBLE
                binding.foodLogLayer.visibility = View.GONE
            }
        }
    }

    private fun fetchWorkoutRecord(selectedDate: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${ServerUrlManager.serverUrl}/workout/record?date=$selectedDate")
            .addHeader("Authorization", "Bearer ${TokenManager.accessToken}")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                //runOnUiThread { showToast("Failed to fetch workout record") }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    //runOnUiThread { showToast("Failed to fetch workout record: ${response.code}") }
                    return
                }

                response.body?.string()?.let { responseBody ->
                    println("출력되는 내용: $responseBody")
                    val record = parseWorkoutRecord(responseBody)
                    runOnUiThread { displayWorkoutRecord(record) }
                }
            }
        })
    }

    private fun parseWorkoutRecord(json: String): WorkoutRecord {
        val jsonObject = JSONObject(json)
        val exerciseTitles = jsonObject.getJSONArray("exerciseTitle").let { array ->
            (0 until array.length()).map { array.getString(it) }
        }
        val totalTime = jsonObject.getInt("time")
        val totalCalorie = jsonObject.getInt("calorie")
        return WorkoutRecord(exerciseTitles, totalTime, totalCalorie)
    }

    private fun displayWorkoutRecord(record: WorkoutRecord) {
        // 운동 시간 및 소모 열량 표시
        binding.exerciseTimeTextView.text = "- 운동 시간: ${record.totalTime}분"
        binding.burnedCaloriesTextView.text = "- 소모 열량: ${record.totalCalorie}kcal"

        // 운동 목록 텍스트 설정
        val exerciseTitles = record.exerciseTitles.joinToString("\n")
        binding.exerciseActivityTextView.text = exerciseTitles
    }


    private fun fetchDietRecord(selectedDate: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${ServerUrlManager.serverUrl}/diet?date=$selectedDate")
            .addHeader("Authorization", "Bearer ${TokenManager.accessToken}")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                //runOnUiThread { showToast("Failed to fetch diet record") }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    //runOnUiThread { showToast("Failed to fetch diet record: ${response.code}") }
                    return
                }

                response.body?.string()?.let { responseBody ->
                    val dietRecord = parseDietRecord(responseBody)
                    runOnUiThread {
                        updateDietRecordUI(dietRecord)
                    }
                    println("식단 일지 관련 데이터: ${responseBody}")
                }
            }
        })
    }

    private fun parseDietRecord(json: String): DietRecord {
        val jsonObject = JSONObject(json)
        val totalCalorie = jsonObject.getInt("totalCalorie")
        val dietInfoArray = jsonObject.getJSONArray("dietInfo")

        val mealTimeCalorieMap = mutableMapOf<String, Int>()
        for (i in 0 until dietInfoArray.length()) {
            val dietObject = dietInfoArray.getJSONObject(i)
            val mealTime = dietObject.getString("mealTime")
            val mealTotalCalorie = dietObject.getInt("totalCalorie")
            val mealTimeKorean = when (mealTime) {
                "BREAKFAST" -> "아침"
                "LUNCH" -> "점심"
                "DINNER" -> "저녁"
                else -> mealTime
            }
            mealTimeCalorieMap[mealTimeKorean] = mealTotalCalorie
        }

        return DietRecord(totalCalorie, mealTimeCalorieMap)
    }

    private fun updateDietRecordUI(dietRecord: DietRecord) {
        val totalCalorie = dietRecord.totalCalorie
        binding.consumedCaloriesTextView.text = "총 섭취 열량: ${totalCalorie}kcal"

        val mealTimeCalorieMap = dietRecord.mealTimeCalorieMap
        val consumedFoodText = mealTimeCalorieMap.entries.joinToString("\n") { "- ${it.key}: ${it.value}kcal" }
        binding.consumedFoodTextView.text = consumedFoodText
    }

}