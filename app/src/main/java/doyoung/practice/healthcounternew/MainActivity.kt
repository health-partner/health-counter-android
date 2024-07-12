package doyoung.practice.healthcounternew

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import doyoung.practice.healthcounternew.databinding.ActivityMainBinding
import doyoung.practice.healthcounternew.databinding.ActivityViewPagerBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TokenManager 초기화
        TokenManager.initialize(this)

        // 리프레시 토큰 확인 후 적절한 액티비티로 전환
        if (TokenManager.refreshToken != null) {
            // 리프레시 토큰이 존재할 경우, 로그아웃하지 않은 사용자가 앱을 다시 구동한 것으로 판단하여 메인 페이지로 이동
            startActivity(Intent(this, ViewPagerActivity::class.java))
            finish()
        } else {
            // 리프레시 토큰이 존재하지 않을 경우, 로그인해야 하는 사용자로 판단하여 로그인 페이지로 이동
            startActivity(Intent(this, LoginActivity2::class.java))
        }

    }

    override fun onResume() {
        super.onResume()

        println("토큰 초기화 전 리프레쉬: ${TokenManager.refreshToken}, 액세스: ${TokenManager.accessToken}")
        // TokenManager 초기화
        TokenManager.initialize(this)
        println("토큰 초기화 후 리프레쉬: ${TokenManager.refreshToken}, 액세스: ${TokenManager.accessToken}")

        // 리프레시 토큰 확인 후 적절한 액티비티로 전환
        if (TokenManager.refreshToken != null) {
            // 리프레시 토큰이 존재할 경우, 로그아웃하지 않은 사용자가 앱을 다시 구동한 것으로 판단하여 메인 페이지로 이동
            startActivity(Intent(this, ViewPagerActivity::class.java))
            finish()
        } else {
            // 리프레시 토큰이 존재하지 않을 경우, 로그인해야 하는 사용자로 판단하여 로그인 페이지로 이동
            startActivity(Intent(this, LoginActivity2::class.java))
        }
    }
}