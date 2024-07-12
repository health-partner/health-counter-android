package doyoung.practice.healthcounternew

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.Toast
import doyoung.practice.healthcounternew.databinding.ActivityEditBinding
import doyoung.practice.healthcounternew.databinding.ActivityEditPersonalInfoBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class EditPersonalInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditPersonalInfoBinding
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPersonalInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 스피너 초기화
        val spinner = binding.exerciseAmountSpinner
        val exerciseAmountChoices = resources.getStringArray(R.array.activity_choices)
        val adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, exerciseAmountChoices)
        spinner.adapter = adapter

        // 저장 플로팅 버튼 동작
        binding.saveButton.setOnClickListener {
            saveData()
            createData()
            finish()
        }
    }

    private fun createData() {
        /*
            서버에 보내는 데이터
            : gender(String), height(Int), weight(Int), goal(Int), activityLevel(Int), recommendedCalories(Int)

            전역 변수에 사용되는 데이터 (하루 표준 열량을 계산하는 데 사용됨)
            : gender(male: 22 / female: 21), height(m 단위), goal(String_증량:+3500kcal/감량:-3500kcal), momentum(활동 계수)
         */

        // 성별_전역 변수
        val gender = getSexType()
        var genderToPost: String
        if(gender == "남성") {
            genderToPost = "MALE"
        } else {
            genderToPost = "FEMALE"
        }


        // 체중
        val weight = getNumericValue(binding.weightEditText.text.toString())

        // 키(m)
        val height = getNumericValue(binding.heightEditText.text.toString())
        // 키(cm)
        val heightCm = height.toFloat() / 100
        // 키(cm)^2_전역 변수
        val heightSquared = heightCm * heightCm


        // 표준 체중의 계산
        val standardWeight = if (gender == "남성") {
            heightSquared * 22
        } else {
            heightSquared * 21
        }

        // goalCheck에 따라 단계별 수행
        val activityLevel = when (getExerciseAmount()) {
            "가벼운 활동 (자주 앉아있음, 일반사무 관리)" -> 1
            "중증도 활동 (자주 서 있음, 서비스업, 판매)" -> 2
            "강한 활동 (많은 활동량, 육체적 노동)" -> 3
            "아주 강한 활동 (운동 선수와 같이 근육을 사용하는 일)" -> 4
            else -> 0 // 기본값 설정
        }

        val activityLevelToGlobal = when (getExerciseAmount()) {
            "가벼운 활동 (자주 앉아있음, 일반사무 관리)" -> 25
            "중증도 활동 (자주 서 있음, 서비스업, 판매)" -> 30
            "강한 활동 (많은 활동량, 육체적 노동)" -> 35
            "아주 강한 활동 (운동 선수와 같이 근육을 사용하는 일)" -> 40
            else -> 0 // 기본값 설정
        }

        val goalCheck = getGoal()
        val goal = when (goalCheck) {
            "감량" -> 1
            "유지" -> 2
            "증량" -> 3
            else -> 0
        }

        // 전역 변수 계산
        val globalCalorie = standardWeight * activityLevelToGlobal
        println("globalCalorie = ${globalCalorie}")
        println("standardWeight = ${standardWeight}, momentumToGlobal = ${activityLevelToGlobal}")
        val globalGoal = when(goalCheck) {
            "감량" -> globalCalorie -500f
            "유지" -> 0.0f
            "증량" -> globalCalorie + 500f
            else -> 0.0f
        }

        // 전역 변수에 할당
        GlobalData.getInstance().setGlobalCalorie(globalCalorie)
        GlobalData.getInstance().setGlobalGoal(globalGoal)

        // 표준 체중과 현재 체중의 비교
        val gapWithStandard = (standardWeight - weight.toFloat()).toInt()
        Toast.makeText(this, "표준 체중과 ${gapWithStandard}kg 차이가 나네요!", Toast.LENGTH_SHORT).show()

        // JSON 객체 생성
        val dataToSend = JSONObject()
        dataToSend.put("gender", genderToPost)
        dataToSend.put("weight", weight)
        dataToSend.put("height", height)
        dataToSend.put("goal", goal)
        dataToSend.put("activityLevel", activityLevel)
        dataToSend.put("recommendedCalories", globalGoal.toInt())

        // 서버에 사용자 정보 전송
        sendDataToServer(dataToSend)
    }

    private fun sendDataToServer(dataToSend: JSONObject) {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = dataToSend.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(ServerUrlManager.serverUrl + "/me")
            .post(requestBody)
            // Bearer 토큰을 Authorization 헤더에 넣습니다.
            .addHeader("Authorization", "Bearer ${TokenManager.accessToken}")
            .addHeader("Accept", "application/json")
            .build()

        val callback = object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Client", "Failed to send data to server: ${e.message}")
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.e("Client", "Data sent successfully!")
                    // 서버로부터 응답을 처리할 수 있습니다. (필요 시)
                } else {
                    Log.e("Client", "Failed to send data to server: ${response.code}")
                }
            }
        }

        client.newCall(request).enqueue(callback)
    }


    // 성별/체중/신장/운동 목표/평소 운동량 저장
    private fun saveData() {
        with(getSharedPreferences(USER_INFORMATION, Context.MODE_PRIVATE).edit()) {
            putString(SEX, getSexType())
            putString(WEIGHT, getNumericValue(binding.weightEditText.text.toString()))
            putString(HEIGHT, getNumericValue(binding.heightEditText.text.toString()))
            putString(GOAL, getGoal())
            putString(EXERCISEAMOUNT, getExerciseAmount())
            apply()
        }
        Toast.makeText(this, "저장을 완료했습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun getNumericValue(input: String): String {
        val regex = Regex("[0-9]+")
        val matchResult = regex.find(input)
        return matchResult?.value ?: ""
    }

    private fun getSexType(): String {
        val selectedSexId = binding.sexRadioGroup.checkedRadioButtonId
        val selectedSex = findViewById<RadioButton>(selectedSexId)
        return selectedSex.text.toString()
    }

    private fun getExerciseAmount(): String {
        val exerciseAmount = binding.exerciseAmountSpinner.selectedItem.toString()
        println("exerciseAmount = ${exerciseAmount}")
        return exerciseAmount
    }

    private fun getGoal(): String {
        val selectedGoalId = binding.goalRadioGroup.checkedRadioButtonId
        val selectedGoal = findViewById<RadioButton>(selectedGoalId)
        return selectedGoal.text.toString()
    }
}