package doyoung.practice.healthcounternew

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import doyoung.practice.healthcounternew.databinding.FragmentExerciseBinding
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class FragmentExercise : Fragment() {

    lateinit var binding: FragmentExerciseBinding
    lateinit var ViewPagerActivity: ViewPagerActivity
    private lateinit var loadedRoutineAdapter: LoadedRoutineAdapter
    private val client = OkHttpClient()

    private var isViewCreated = false
    private var isDataLoaded = false

    private val dummyList = mutableListOf<String>() // selectedExercises를 클래스 변수로 선언

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is ViewPagerActivity) ViewPagerActivity = context
    }

    override fun onResume() {
        super.onResume()
        // Fragment가 다시 활성화될 때 데이터를 다시 로드
        fetchRoutinesFromServer()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentExerciseBinding.inflate(inflater, container, false)

        isViewCreated = true
        loadDataIfVisible()

        return binding.root
    }

    private fun loadDataIfVisible() {
        if (isViewCreated && !isDataLoaded) {
            initRecyclerView()

            // 서버로부터 받은 루틴들을 화면에 출력
            fetchRoutinesFromServer()
            isDataLoaded = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewPagerActivity = activity as ViewPagerActivity
        val deviceAddress = viewPagerActivity.getBluetoothDeviceAddress()

        // 바인딩 로직
        binding.addButton.setOnClickListener {
            val intent = Intent(context, RoutineSetting2Activity::class.java)
            intent.putExtra(RoutineSetting2Activity.EXTRAS_DEVICE_ADDRESS, deviceAddress)
            startActivity(intent)
        }
    }

    private fun initRecyclerView() {
        loadedRoutineAdapter = LoadedRoutineAdapter(mutableListOf()).apply {
            onItemClick = { routineId ->

                val viewPagerActivity = activity as ViewPagerActivity
                val deviceAddress = viewPagerActivity.getBluetoothDeviceAddress()

                val intent = Intent(context, RoutineSetting2Activity::class.java)
                // Ble 작동을 위해 디바이스 주소를 넘김
                intent.putExtra(RoutineSetting2Activity.EXTRAS_DEVICE_ADDRESS, deviceAddress)

                // 루틴 Id를 통해 루틴을 불러오도록 routineId 인텐트에 담아 넘기기
                intent.putExtra("routineId", routineId + 1)
                startActivity(intent)
            }
        }

        binding.loadedRoutineRecyclerView.apply {
            adapter = loadedRoutineAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            val dividerItemDecoration = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
            addItemDecoration(dividerItemDecoration)
        }
    }

    private fun fetchRoutinesFromServer() {
        val request = Request.Builder()
            .url(ServerUrlManager.serverUrl + "/routines")
            .addHeader("Authorization", "Bearer ${TokenManager.accessToken}")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e("FragmentExercise", "Failed to load routines", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    showToast("Failed to load routines: ${response.code}")
                    Log.e("FragmentExercise", "Failed to load routines: ${response.code}")
                    return
                }

                response.body?.string()?.let { responseBody ->
                    Log.d("FragmentExercise", "Server response: $responseBody")
                    val routines = parseRoutines(responseBody)
                    activity?.runOnUiThread {
                        loadedRoutineAdapter.setData(routines)
                    }
                }
            }
        })
    }

    private fun parseRoutines(json: String): List<ExerciseRecycle> {
        val routineList = mutableListOf<ExerciseRecycle>()

        try {
            val jsonObject = JSONObject(json)
            val routinesArray: JSONArray = jsonObject.getJSONArray("routines")

            for (i in 0 until routinesArray.length()) {
                val routineObject = routinesArray.getJSONObject(i)
                val routineTitle = routineObject.getString("routineTitle")
                val routineId = routineObject.getInt("routineId")
                routineList.add(ExerciseRecycle(routineTitle, routineId))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Check if routineList is empty and add a message if it is
        if (routineList.isEmpty()) {
            routineList.add(ExerciseRecycle("아직 등록된 루틴이 없습니다", -1))
        }

        return routineList
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}