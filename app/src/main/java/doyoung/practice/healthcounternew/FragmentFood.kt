package doyoung.practice.healthcounternew

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import doyoung.practice.healthcounternew.databinding.FragmentExerciseBinding
import doyoung.practice.healthcounternew.databinding.FragmentFoodBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class FragmentFood : Fragment() {

    lateinit var binding: FragmentFoodBinding
    lateinit var ViewPagerActivity: ViewPagerActivity
    private var dietInfo: List<Map<String, Any>> = emptyList()

    private val client = OkHttpClient()

    private var isViewCreated = false
    private var isDataLoaded = false

    // 오늘 날짜 계산
    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is ViewPagerActivity) ViewPagerActivity = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFoodBinding.inflate(inflater, container, false)
        isViewCreated = true
        loadDataIfVisible()

        return binding.root
    }

    private fun loadDataIfVisible() {
        if (isViewCreated && !isDataLoaded) {
            // 데이터를 서버에서 받아와서 설정
            fetchAndUpdateCalories(todayDate)
            isDataLoaded = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바인딩 로직
        // 각각의 추가 버튼 클릭 시의 로직
        binding.breakfastAddButton.setOnClickListener {
            val intent = Intent(context, CalorieActivity::class.java)
            intent.putExtra("whatFood", "아침")
            startActivity(intent)
        }

        binding.lunchAddButton.setOnClickListener{
            val intent = Intent(context, CalorieActivity::class.java)
            intent.putExtra("whatFood", "점심")
            startActivity(intent)
        }

        binding.dinnerAddButton.setOnClickListener {
            val intent = Intent(context, CalorieActivity::class.java)
            intent.putExtra("whatFood", "저녁")
            startActivity(intent)
        }


        binding.leftArrowButton1.setOnClickListener {
            with(binding) {
                breakfastCalorieTextView.visibility = View.GONE
                breakfastMenuTextView.visibility = View.GONE
                leftArrowButton1.visibility = View.INVISIBLE
                downArrowButton1.visibility = View.VISIBLE
            }
        }

        binding.downArrowButton1.setOnClickListener {
            getFromServer(todayDate,"BREAKFAST", binding.breakfastCalorieTextView, binding.breakfastMenuTextView, binding.leftArrowButton1, binding.downArrowButton1)
        }

        binding.leftArrowButton1.setOnClickListener {
            with(binding) {
                breakfastCalorieTextView.visibility = View.GONE
                breakfastMenuTextView.visibility = View.GONE
                leftArrowButton1.visibility = View.INVISIBLE
                downArrowButton1.visibility = View.VISIBLE
            }
        }

        binding.downArrowButton2.setOnClickListener {
            getFromServer(todayDate, "LUNCH", binding.lunchCalorieTextView, binding.lunchMenuTextView, binding.leftArrowButton2, binding.downArrowButton2)
        }

        binding.leftArrowButton2.setOnClickListener {
            with(binding) {
                lunchCalorieTextView.visibility = View.GONE
                lunchMenuTextView.visibility = View.GONE
                leftArrowButton2.visibility = View.INVISIBLE
                downArrowButton2.visibility = View.VISIBLE
            }
        }

        binding.downArrowButton3.setOnClickListener {
            getFromServer(todayDate, "DINNER", binding.dinnerCalorieTextView, binding.dinnerMenuTextView, binding.leftArrowButton3, binding.downArrowButton3)
        }

        binding.leftArrowButton3.setOnClickListener {
            with(binding) {
                dinnerCalorieTextView.visibility = View.GONE
                dinnerMenuTextView.visibility = View.GONE
                leftArrowButton3.visibility = View.INVISIBLE
                downArrowButton3.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isDataLoaded = false // 데이터가 다시 로드되도록 설정
        loadDataIfVisible()
    }

    private fun getFromServer(date: String, mealTime: String, calorieTextView: TextView, menuTextView: TextView, leftArrowButton: View, downArrowButton: View) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${ServerUrlManager.serverUrl}/diet?date=$date")
            .addHeader("Authorization", "Bearer ${TokenManager.accessToken}")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                showToast("Failed to fetch diet record")
                println("Diet record fetch failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    showToast("Failed to fetch diet record: ${response.code}")
                    println("Diet record fetch failed with response code: ${response.code}")
                    return
                }

                response.body?.string()?.let { responseBody ->
                    try {
                        val json = JSONObject(responseBody)
                        val totalCalorie = json.optInt("totalCalorie", 0)
                        val dietArray = json.optJSONArray("dietInfo")

                        val tempList = mutableListOf<Map<String, Any>>()
                        if (dietArray != null) {
                            for (i in 0 until dietArray.length()) {
                                val dietObject = dietArray.optJSONObject(i)
                                if (dietObject != null) {
                                    val mealTimeFromServer = dietObject.optString("mealTime", "")
                                    if (mealTimeFromServer == mealTime) {
                                        val mealTotalCalorie = dietObject.optInt("totalCalorie", 0)
                                        val dishArray = dietObject.optJSONArray("dish")
                                        val dishes = mutableListOf<String>()
                                        if (dishArray != null) {
                                            for (j in 0 until dishArray.length()) {
                                                dishes.add(dishArray.optString(j, ""))
                                            }
                                        }
                                        tempList.add(
                                            mapOf(
                                                "mealTime" to mealTimeFromServer,
                                                "totalCalorie" to mealTotalCalorie,
                                                "dishes" to dishes
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        dietInfo = tempList
                        Handler(Looper.getMainLooper()).post {
                            displayDietInfo(mealTime, calorieTextView, menuTextView, leftArrowButton, downArrowButton)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        showToast("Failed to parse diet record")
                        println("Diet record parsing failed: ${e.message}")
                    }
                } ?: run {
                    showToast("Response body is null")
                    println("Diet record response body is null")
                }
            }
        })
    }

    private fun fetchAndUpdateCalories(todayDate: String) {
        // Fetch user status
        fetchUserStatus { recommendedCalories ->
            Handler(Looper.getMainLooper()).post {
                val recommendedCalorieText = recommendedCalories * 3
                binding.recommendedTextView.text = "${recommendedCalorieText} kcal"
            }

            // Fetch diet and workout records
            fetchDietRecord(todayDate) { dietCalories ->
                Handler(Looper.getMainLooper()).post {
                    binding.consumedTextView.text = "${dietCalories} kcal"
                }

                // Fetch workout record after diet record is fetched
                fetchWorkoutRecord(todayDate) { workoutCalories ->
                    Handler(Looper.getMainLooper()).post {
                        binding.exerciseTextView.text = "${workoutCalories} kcal"
                    }

                    // Calculate and update remainTextView
                    val remainCalories = recommendedCalories - dietCalories + workoutCalories
                    Handler(Looper.getMainLooper()).post {
                        binding.remainTextView.text = "${remainCalories} kcal"
                    }
                }
            }
        }
    }

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
                    println("FOOD-RECOMMEND: ${recommendedCalories}")
                    Handler(Looper.getMainLooper()).post {
                        callback(recommendedCalories)
                    }
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
                println("Diet record fetch failed: ${e.message}")
                callback(0) // Default to 0 if request fails
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    showToast("Failed to fetch diet record: ${response.code}")
                    println("Diet record fetch failed with response code: ${response.code}")
                    return
                }

                response.body?.string()?.let { responseBody ->
                    try {
                        val json = JSONObject(responseBody)
                        val totalCalorie = json.optInt("totalCalorie", 0) // totalCalorie 값을 추출
                        println("Fetched totalCalorie: $totalCalorie")
                        callback(totalCalorie)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        showToast("Failed to parse diet record")
                        println("Diet record parsing failed: ${e.message}")
                        callback(0) // Default to 0 if parsing fails
                    }
                } ?: run {
                    showToast("Response body is null")
                    println("Diet record response body is null")
                    callback(0) // Default to 0 if response body is null
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
                println("Workout record fetch failed: ${e.message}")
                callback(0) // Default to 0 if request fails
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    showToast("Failed to fetch workout record: ${response.code}")
                    println("Workout record fetch failed with response code: ${response.code}")
                    callback(0) // Default to 0 if request fails
                    return
                }

                response.body?.string()?.let { responseBody ->
                    try {
                        val json = JSONObject(responseBody)
                        val calorie = json.optInt("calorie", 0) // calorie 값을 추출
                        println("Fetched calorie: $calorie")
                        callback(calorie)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        showToast("Failed to parse workout record")
                        println("Workout record parsing failed: ${e.message}")
                        callback(0) // Default to 0 if parsing fails
                    }
                } ?: run {
                    showToast("Response body is null")
                    println("Workout record response body is null")
                    callback(0) // Default to 0 if response body is null
                }
            }
        })
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayDietInfo(mealTime: String, calorieTextView: TextView, menuTextView: TextView, leftArrowButton: View, downArrowButton: View) {
        val diet = dietInfo.find { it["mealTime"] == mealTime }
        if (diet != null) {
            val totalCalorie = diet["totalCalorie"] as Int
            val dishes = diet["dishes"] as List<String>
            val formattedDishes = formatDishes(dishes)

            calorieTextView.text = "총 섭취량: ${totalCalorie} kcal"
            menuTextView.text = formattedDishes
        } else {
            calorieTextView.text = "아직 기록이 없습니다"
            menuTextView.text = ""
        }

        downArrowButton.visibility = View.GONE
        leftArrowButton.visibility = View.VISIBLE
        calorieTextView.visibility = View.VISIBLE
        menuTextView.visibility = View.VISIBLE
    }

    private fun formatDishes(dishes: List<String>): String {
        val result = StringBuilder()
        for (i in dishes.indices step 4) {
            val endIndex = (i + 4).coerceAtMost(dishes.size)
            val menuGroup = dishes.subList(i, endIndex).joinToString(", ")
            result.append("$menuGroup\n")
        }
        return result.toString()
    }
}
