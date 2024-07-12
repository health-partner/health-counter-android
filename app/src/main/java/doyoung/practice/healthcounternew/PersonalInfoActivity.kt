package doyoung.practice.healthcounternew

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import doyoung.practice.healthcounternew.databinding.ActivityPersonalInfoBinding

class PersonalInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonalInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonalInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.editButton.setOnClickListener {
            startActivity(Intent(this, EditActivity::class.java))
        }

        binding.goToMainButton.setOnClickListener {
            Toast.makeText(this, "오늘도 목표를 향해 열심히 달려보아요!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, ViewPagerActivity::class.java))
        }

    }

    override fun onResume() {
        super.onResume()
        getDataUiUpdate()
    }

    // EditActivity로부터 얻은 값을 띄우기
    private fun getDataUiUpdate() {
        with(getSharedPreferences(USER_INFORMATION, Context.MODE_PRIVATE)) {
            val sexValue = getString(SEX, "미등록")
            binding.sex.text = if(sexValue == "남성") "남 성" else if (sexValue == "여성") "여 성" else "미등록"
            binding.weight.text = getString(WEIGHT, "미등록")
            binding.height.text = getString(HEIGHT, "미등록")
            binding.goal.text = getString(GOAL, "미등록")
            binding.exerciseAmount.text = getString(EXERCISEAMOUNT, "미등록")
            if(binding.weight.text != "미등록") {
                binding.kilogram.visibility = View.VISIBLE
            }
            if(binding.height.text != "미등록") {
                binding.centimeter.visibility = View.VISIBLE
            }
            if(binding.goal.text != "미등록") {
                binding.goalText.visibility = View.VISIBLE
            }
        }
    }
}