package doyoung.practice.healthcounternew

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import doyoung.practice.healthcounternew.databinding.ActivityRoutineSettingBinding

class RoutineSettingActivity : AppCompatActivity(), ExerciseAdapter.ItemClickListener {

    private lateinit var binding: ActivityRoutineSettingBinding
    private lateinit var exerciseAdapter: ExerciseAdapter

    // 액티비티 간 데이터 송수신
    lateinit var resultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutineSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        }

        initRecyclerView()

        // ActivityResult의 수신
        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if(result.resultCode == RESULT_OK) {
                    val name = result.data?.getStringExtra("exerciseName") ?: ""

                    // 텍스트를 받아온 운동 이름으로 더미리스트 업데이트
                    updateDataList(name)
                }
            }
    }

    override fun onClick(exercise: Exercise) {
        //Toast.makeText(this, "${exercise.name}가 클릭됐습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun initRecyclerView() {

        // 더미리스트
        val dummyList = mutableListOf<Exercise>(

        )

        exerciseAdapter = ExerciseAdapter(dummyList, this)
        // 리사이클러뷰의 설정
        binding.exerciseRecyclerView.apply {
            adapter = exerciseAdapter
            layoutManager = LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
            val dividerItemDecoration = DividerItemDecoration(applicationContext, LinearLayoutManager.VERTICAL)
            addItemDecoration(dividerItemDecoration)
        }
    }

    private fun updateDataList(exerciseName: String) {
        val updatedDataList = mutableListOf<Exercise>()
        for(exercise in exerciseAdapter.getList()) {
            updatedDataList.add(exercise)
        }
        updatedDataList.add(Exercise(exerciseName))

        exerciseAdapter.setList(updatedDataList)
        exerciseAdapter.notifyDataSetChanged()
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
            val exerciseNames = exerciseAdapter.getList().map { it.name }
            val intent = Intent(this, RoutineSaveActivity::class.java)
            intent.putStringArrayListExtra("exerciseNames", ArrayList(exerciseNames))
            //println(ArrayList(exerciseNames))
            startActivity(intent)
        }

        builder.setNegativeButton("아니오") { dialog, which ->
            showToast("운동을 마칩니다..")
            // 기존 홈페이지로 이동
            startActivity(Intent(this, ViewPagerActivity::class.java))
        }

        builder.show()
    }

    // 토스트 메시지 형식
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}