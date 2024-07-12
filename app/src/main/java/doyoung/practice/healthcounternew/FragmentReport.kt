package doyoung.practice.healthcounternew

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import doyoung.practice.healthcounternew.databinding.FragmentHomeBinding
import doyoung.practice.healthcounternew.databinding.FragmentReportBinding
import okhttp3.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class FragmentReport : Fragment() {

    lateinit var binding: FragmentReportBinding
    lateinit var ViewPagerActivity: ViewPagerActivity

    // 오늘 날짜 계산
    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

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

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is ViewPagerActivity) ViewPagerActivity = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바인딩 로직
        setUpChart()

        // 스피너
        val spinner = binding.exerciseSelectedSpinner
        val exerciseSelected = resources.getStringArray(R.array.exercise_selected_none)
        val adapter =
            context?.let {
                ArrayAdapter(
                    it,
                    android.R.layout.simple_spinner_dropdown_item,
                    exerciseSelected
                )
            }
        spinner.adapter = adapter

        /*

            스피너에서 클릭된 아이템의 이름을 바탕으로 매핑된 exerciseId를 알아내고,
            서버의 /workout/training-volume?exerciseId=XX&date=OO
            엔드포인트로 GET 요청하기

         */

        // 스피너에 OnItemSelectedListener 추가
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedText = parent.getItemAtPosition(position).toString()
                println("선택된 운동: ${selectedText}")

                // 선택된 운동에 대한 exerciseId 가져오기
                val exerciseId = exerciseIdMap[selectedText] ?: return

                when(exerciseId) {
                    101 -> {setUpChest()}
                    102 -> {setUpChest()}
                    103 -> {setUpChest()}
                    104 -> {setUpChest()}
                    201 -> {setUpShoulder()}
                    202 -> {setUpShoulder()}
                    203 -> {setUpShoulder()}
                    204 -> {setUpShoulder()}
                    301 -> {setUpLeg()}
                    302 -> {setUpLeg()}
                    303 -> {setUpLeg()}
                    304 -> {setUpLeg()}
                    401 -> {setUpBack()}
                    402 -> {setUpBack()}
                    403 -> {setUpBack()}
                    404 -> {setUpBack()}
                    else -> {}
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }


        // 라디오 그룹에 대한 리스너
        binding.exerciseRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            val exerciseArray = when (checkedId) {
                R.id.chestRadioButton -> R.array.exercise_selected_chest
                R.id.shoulderRadioButton -> R.array.exercise_selected_shoulder
                R.id.legRadioButton -> R.array.exercise_selected_leg
                R.id.backRadioButton -> R.array.exercise_selected_back
                else -> R.array.exercise_selected_none
            }

            val newExerciseSelected = resources.getStringArray(exerciseArray)
            val newAdapter = context?.let {
                ArrayAdapter(
                    it,
                    android.R.layout.simple_spinner_dropdown_item,
                    newExerciseSelected
                )
            }
            spinner.adapter = newAdapter
        }

    }


    override fun onResume() {
        super.onResume()
        updateChartData()
    }

    private fun setUpChart() {
        // 운동 볼륨 그래프 설정
        val exerciseVolumeChart: LineChart = binding.exerciseVolumeChart
        val entries = mutableListOf<Entry>()
        for (i in 1..31) {
            val randomY = 0f // Y 값을 0으로 설정
            entries.add(Entry(i.toFloat(), randomY))
        }

        val dataSet = LineDataSet(entries, "운동 볼륨의 추이")
        val lineData = LineData(dataSet)

        exerciseVolumeChart.data = lineData
        exerciseVolumeChart.axisLeft.axisMinimum = 1000f
        exerciseVolumeChart.axisLeft.axisMaximum = 2000f

        val description = Description()
        description.text = "날짜"
        exerciseVolumeChart.description = description

        exerciseVolumeChart.xAxis.setDrawGridLines(true)
        exerciseVolumeChart.xAxis.granularity = 2f
        exerciseVolumeChart.invalidate()

        // 식단 그래프 설정
        val calorieChart: LineChart = binding.calorieChart
        val calorieEntries = mutableListOf<Entry>()
        for (i in 1..31) {
            val randomY = Random.nextInt(1700, 2700).toFloat()
            calorieEntries.add(Entry(i.toFloat(), randomY))
        }

        val calorieDataSet = LineDataSet(calorieEntries, "하루 섭취량(kcal)")
        calorieDataSet.color = Color.BLUE
        calorieDataSet.setCircleColor(Color.BLUE)
        calorieDataSet.lineWidth = 2f
        calorieDataSet.circleRadius = 3f

        val calorieLineData = LineData(calorieDataSet)

        // 두 번째 데이터 세트 추가 (Global Goal)
        val globalGoalEntries = mutableListOf<Entry>()
        val globalGoal = GlobalData.getInstance().getGlobalGoal()

        for (i in 1..31) {
            globalGoalEntries.add(Entry(i.toFloat(), globalGoal.toFloat()))
        }

        val globalGoalDataSet = LineDataSet(globalGoalEntries, "목표 칼로리")
        globalGoalDataSet.color = Color.RED
        globalGoalDataSet.lineWidth = 2f
        globalGoalDataSet.setDrawCircles(false)
        globalGoalDataSet.setDrawValues(false)

        calorieLineData.addDataSet(globalGoalDataSet)

        calorieChart.data = calorieLineData

        // Y축 범위를 Global Goal 기준으로 플러스/마이너스 1300으로 설정
        val yAxisMin = globalGoal - 1000
        val yAxisMax = globalGoal + 1000
        calorieChart.axisLeft.axisMinimum = yAxisMin.toFloat()
        calorieChart.axisLeft.axisMaximum = yAxisMax.toFloat()

        val calorieDescription = Description()
        calorieDescription.text = "날짜"
        calorieChart.description = calorieDescription

        calorieChart.xAxis.setDrawGridLines(true)
        calorieChart.xAxis.granularity = 2f
        calorieChart.invalidate()
    }

    private fun updateChartData() {
        val calorieChart: LineChart = binding.calorieChart
        val calorieLineData = calorieChart.data

        // 기존의 모든 데이터 세트를 제거
        calorieLineData.clearValues()

        // 새로운 하루 섭취량(kcal) 데이터 세트 추가
        val otherEntries = mutableListOf<Entry>()
        val otherYValues = listOf(
            2800f, 2850f, 3100f, 2500f, // 6월 1일 - 4일
            2700f, 2800f, 3400f, 2500f, // 6월 5일 - 8일
            2900f, 2850f, 2840f, 2700f, // 6월 9일 - 12일
            2758f, 2840f, 2810f, 2880f, // 6월 13일 - 16일
            2890f, 2910f, 2950f, 2801f, // 6월 17일 - 20일
            2840f, 2870f, 2870f, 2880f  // 6월 21일 - 24일
        )

        for (i in otherYValues.indices) {
            otherEntries.add(Entry((i + 1).toFloat(), otherYValues[i]))
        }

        val otherDataSet = LineDataSet(otherEntries, "하루 섭취량(kcal)")
        otherDataSet.color = Color.BLUE // 다른 색상 설정
        otherDataSet.lineWidth = 3f // 선 굵기 설정
        otherDataSet.setDrawCircles(false) // 원 숨기기
        otherDataSet.setDrawValues(false) // 값 숨기기

        calorieLineData.addDataSet(otherDataSet)

        // 새로운 Global Goal 데이터 세트 추가 (Red 색상 데이터)
        val globalGoalEntries = mutableListOf<Entry>()
        val globalGoal = 2829f

        for (i in 1..31) {
            globalGoalEntries.add(Entry(i.toFloat(), globalGoal))
        }

        val globalGoalDataSet = LineDataSet(globalGoalEntries, "목표 칼로리")
        globalGoalDataSet.color = Color.RED
        globalGoalDataSet.lineWidth = 3f // 선 굵기 설정
        globalGoalDataSet.setDrawCircles(false)
        globalGoalDataSet.setDrawValues(false) // 값 숨기기

        calorieLineData.addDataSet(globalGoalDataSet)

        // Y축 범위를 2400 - 3000으로 설정
        calorieChart.axisLeft.axisMinimum = 0f
        calorieChart.axisLeft.axisMaximum = 6000f

        calorieChart.notifyDataSetChanged()
        calorieChart.invalidate()
    }




    /*

        운동 종목별 볼륨 계산 관련 함수

        가슴: setUpChest()
        어깨: setUpShoulder()
        하체: setUpLeg()
        등: setUpBack()

     */

    private fun setUpChest() {
        // 운동 볼륨 그래프 설정
        val exerciseVolumeChart: LineChart = binding.exerciseVolumeChart
        val entries = mutableListOf<Entry>()
        val highlightEntries = mutableListOf<Entry>()

        val chestVolumes = listOf(
            2480f, 2480f, 2480f, 2480f, // 6월 1일 - 4일
            2320f, 2320f, 2320f, 2320f, // 6월 5일 - 8일
            2530f, 2530f, 2530f, 2530f, // 6월 9일 - 12일
            2618f, 2618f, 2618f, 2618f, // 6월 13일 - 16일
            2730f, 2730f, 2730f, 2730f, // 6월 17일 - 20일
            2640f, 2640f, 2640f, 2640f  // 6월 21일 - 24일
        )

        for (i in chestVolumes.indices) {
            val entry = Entry((i + 1).toFloat(), chestVolumes[i])
            entries.add(entry)
            // 특정 날짜에만 값을 표시하도록 설정
            if (i % 4 == 0) {
                highlightEntries.add(entry)
            }
        }

        val dataSet = LineDataSet(entries, "바벨 플랫 벤치 프레스 볼륨의 추이")
        dataSet.setDrawValues(false) // 모든 값 표시 비활성화
        dataSet.setDrawCircles(false) // 모든 원 표시 비활성화
        dataSet.lineWidth = 3f // 선 굵기 설정


        val lineData = LineData(dataSet)

        exerciseVolumeChart.data = lineData
        exerciseVolumeChart.axisLeft.axisMinimum = 2000f
        exerciseVolumeChart.axisLeft.axisMaximum = 3000f

        val description = Description()
        description.text = "날짜"
        exerciseVolumeChart.description = description

        exerciseVolumeChart.xAxis.setDrawGridLines(true)
        exerciseVolumeChart.xAxis.granularity = 1f
        exerciseVolumeChart.invalidate()
    }


    private fun setUpShoulder() {
        // 운동 볼륨 그래프 설정
        val exerciseVolumeChart: LineChart = binding.exerciseVolumeChart
        val entries = mutableListOf<Entry>()
        val highlightEntries = mutableListOf<Entry>()

        val chestVolumes = listOf(
            0f, 2530f, 2530f, 2530f, // 6월 1일 - 4일
            2530f, 2650f, 2650f, 2650f, // 6월 5일 - 8일
            2650f, 2730f, 2730f, 2730f, // 6월 9일 - 12일
            2730f, 2870f, 2870f, 2870f, // 6월 13일 - 16일
            2870f, 3040f, 3040f, 3040f, // 6월 17일 - 20일
            3040f, 3200f, 3200f, 3200f  // 6월 21일 - 24일
        )

        for (i in chestVolumes.indices) {
            val entry = Entry((i + 1).toFloat(), chestVolumes[i])
            entries.add(entry)
            // 특정 날짜에만 값을 표시하도록 설정
            if (i % 4 == 0) {
                highlightEntries.add(entry)
            }
        }

        val dataSet = LineDataSet(entries, "바벨 숄더 프레스 볼륨의 추이")
        dataSet.setDrawValues(false) // 모든 값 표시 비활성화
        dataSet.setDrawCircles(false) // 모든 원 표시 비활성화
        dataSet.lineWidth = 3f // 선 굵기 설정


        val lineData = LineData(dataSet)

        exerciseVolumeChart.data = lineData
        exerciseVolumeChart.axisLeft.axisMinimum = 2300f
        exerciseVolumeChart.axisLeft.axisMaximum = 3500f

        val description = Description()
        description.text = "날짜"
        exerciseVolumeChart.description = description

        exerciseVolumeChart.xAxis.setDrawGridLines(true)
        exerciseVolumeChart.xAxis.granularity = 1f
        exerciseVolumeChart.invalidate()
    }

    private fun setUpLeg() {
        // 운동 볼륨 그래프 설정
        val exerciseVolumeChart: LineChart = binding.exerciseVolumeChart
        val entries = mutableListOf<Entry>()
        val highlightEntries = mutableListOf<Entry>()

        val chestVolumes = listOf(
            0f, 0f, 2840f, 2840f, // 6월 1일 - 4일
            2840f, 2840f, 2980f, 2980f, // 6월 5일 - 8일
            2980f, 2980f, 2780f, 2780f, // 6월 9일 - 12일
            2780f, 2780f, 3100f, 3100f, // 6월 13일 - 16일
            3100f, 3100f, 3000f, 3000f, // 6월 17일 - 20일
            3000f, 3000f, 3300f, 3300f  // 6월 21일 - 24일
        )

        for (i in chestVolumes.indices) {
            val entry = Entry((i + 1).toFloat(), chestVolumes[i])
            entries.add(entry)
            // 특정 날짜에만 값을 표시하도록 설정
            if (i % 4 == 0) {
                highlightEntries.add(entry)
            }
        }

        val dataSet = LineDataSet(entries, "바벨 스쿼트 볼륨의 추이")
        dataSet.setDrawValues(false) // 모든 값 표시 비활성화
        dataSet.setDrawCircles(false) // 모든 원 표시 비활성화
        dataSet.lineWidth = 3f // 선 굵기 설정


        val lineData = LineData(dataSet)

        exerciseVolumeChart.data = lineData
        exerciseVolumeChart.axisLeft.axisMinimum = 2600f
        exerciseVolumeChart.axisLeft.axisMaximum = 3500f

        val description = Description()
        description.text = "날짜"
        exerciseVolumeChart.description = description

        exerciseVolumeChart.xAxis.setDrawGridLines(true)
        exerciseVolumeChart.xAxis.granularity = 1f
        exerciseVolumeChart.invalidate()
    }

    private fun setUpBack() {
        // 운동 볼륨 그래프 설정
        val exerciseVolumeChart: LineChart = binding.exerciseVolumeChart
        val entries = mutableListOf<Entry>()
        val highlightEntries = mutableListOf<Entry>()

        val chestVolumes = listOf(
            0f, 0f, 0f, 2450f, // 6월 1일 - 4일
            2450f, 2450f, 2450f, 2500f, // 6월 5일 - 8일
            2500f, 2500f, 2500f, 2550f, // 6월 9일 - 12일
            2550f, 2550f, 2550f, 2490f, // 6월 13일 - 16일
            2490f, 2490f, 2490f, 2600f, // 6월 17일 - 20일
            2600f, 2600f, 2600f, 2640f  // 6월 21일 - 24일
        )

        for (i in chestVolumes.indices) {
            val entry = Entry((i + 1).toFloat(), chestVolumes[i])
            entries.add(entry)
            // 특정 날짜에만 값을 표시하도록 설정
            if (i % 4 == 0) {
                highlightEntries.add(entry)
            }
        }

        val dataSet = LineDataSet(entries, "랫 풀 다운 볼륨의 추이")
        dataSet.setDrawValues(false) // 모든 값 표시 비활성화
        dataSet.setDrawCircles(false) // 모든 원 표시 비활성화
        dataSet.lineWidth = 3f // 선 굵기 설정


        val lineData = LineData(dataSet)

        exerciseVolumeChart.data = lineData
        exerciseVolumeChart.axisLeft.axisMinimum = 2200f
        exerciseVolumeChart.axisLeft.axisMaximum = 2900f

        val description = Description()
        description.text = "날짜"
        exerciseVolumeChart.description = description

        exerciseVolumeChart.xAxis.setDrawGridLines(true)
        exerciseVolumeChart.xAxis.granularity = 1f
        exerciseVolumeChart.invalidate()
    }
}