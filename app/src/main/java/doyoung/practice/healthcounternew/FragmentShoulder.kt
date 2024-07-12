package doyoung.practice.healthcounternew

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import doyoung.practice.healthcounternew.databinding.FragmentShoulderBinding

class FragmentShoulder: Fragment() {
    lateinit var binding: FragmentShoulderBinding
    lateinit var selectActivity: ExerciseSelectActivity

    // 액티비티로 넘겨줄 운동명을 담은 값 쓸지 안쓸지 모르겠음
    // private lateinit var text: String

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if(context is ExerciseSelectActivity) selectActivity = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentShoulderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setLayerClickListeners()
        setFloatingActionButtonClickListener()

        // 추가적인 바인딩 관련 처리
    }

    // 라디오그룹을 관리하기 위한 함수
    private fun setLayerClickListeners() {
        val layerIds = arrayOf(
            R.id.ex01Layer,
            R.id.ex02Layer,
            R.id.ex03Layer,
            R.id.ex04Layer
        )

        val radioButtons = arrayOf(
            binding.radioButton1,
            binding.radioButton2,
            binding.radioButton3,
            binding.radioButton4
        )

        for (i in layerIds.indices) {
            binding.root.findViewById<View>(layerIds[i]).setOnClickListener {
                radioButtons[i].isChecked = true
            }
        }
    }

    // 선택된 운동을 상위 액티비티에 전달하기 위한 함수
    private fun setFloatingActionButtonClickListener() {
        binding.exerciseAddButton.setOnClickListener {
            val selectedExerciseName = when {
                binding.radioButton1.isChecked -> binding.ex01TextView.text.toString()
                binding.radioButton2.isChecked -> binding.ex02TextView.text.toString()
                binding.radioButton3.isChecked -> binding.ex03TextView.text.toString()
                binding.radioButton4.isChecked -> binding.ex04TextView.text.toString()
                else -> ""
            }

            // 해당 함수는 상위 액티비티에 정의되어 있음.
            selectActivity.handleExerciseAddButtonClick(selectedExerciseName)
        }
    }
}