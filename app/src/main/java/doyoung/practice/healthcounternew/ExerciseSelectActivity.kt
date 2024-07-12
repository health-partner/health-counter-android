package doyoung.practice.healthcounternew

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import doyoung.practice.healthcounternew.databinding.ActivityExerciseSelectBinding

class ExerciseSelectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExerciseSelectBinding
    private val selectedExercises = arrayListOf<String>() // 선택된 항목들을 저장할 리스트


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.goToSetting.setOnClickListener {
            finish()
        }


        val fragmentList =
            listOf(FragmentChest(),FragmentShoulder(),FragmentLeg(),FragmentBack())
        val pagerAdapter = SelectPagerAdapter(fragmentList, this)
        binding.selectViewPager.adapter = pagerAdapter
        val tabList = listOf("가슴", "어깨", "하체", "등")

        TabLayoutMediator(binding.tabLayout, binding.selectViewPager) { tab, position ->
            tab.text = tabList[position]
        }.attach()
    }

    class SelectPagerAdapter(val fragmentList: List<Fragment>, fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount() = fragmentList.size

        override fun createFragment(position: Int) = fragmentList[position]
    }


    fun handleExerciseAddButtonClick(selectedExerciseName: String) {
        Toast.makeText(this, "종목을 추가했습니다.", Toast.LENGTH_SHORT).show()
        intent.putExtra("exerciseName", selectedExerciseName)
        setResult(RESULT_OK, intent)
        finish()
    }

    /**
    fun handleExerciseAddButtonClick(selectedExerciseName: String) {
        selectedExercises.add(selectedExerciseName) // 선택된 항목을 리스트에 추가
        Toast.makeText(this, "종목을 추가했습니다.", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, RoutineSetting2Activity::class.java).apply {
            putStringArrayListExtra("selectedExercises", selectedExercises) // 전체 리스트를 전달
        }
        println("Selected Exercises to pass: $selectedExercises")
        setResult(RESULT_OK, intent)
        startActivity(intent) // Activity 시작
        finish()

        selectedExercises.add(selectedExerciseName) // 선택된 항목을 리스트에 추가
    } **/
}