package doyoung.practice.healthcounternew

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import doyoung.practice.healthcounternew.databinding.ActivityLogin2Binding
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.CookieManager

class LoginActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivityLogin2Binding
    private val cookieManager by lazy { CookieManager.getDefault() }
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogin2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.kakaoTalkLoginButton.setOnClickListener {
            binding.loginWebView.apply {
                webViewClient =
                    LoginWebViewClient(binding.progressBar, this) { accessToken, refreshToken ->
                        accessToken?.let {
                            // 토큰을 TokenManager에 저장
                            TokenManager.saveTokens(this@LoginActivity2, it, refreshToken)

                            // 서버에 상태 요청 보내기
                            checkUserStatus()
                        } ?: run {
                            showToast("Failed to get tokens")
                        }
                    }
                settings.javaScriptEnabled = true // JavaScript 활성화
                settings.domStorageEnabled = true // DOM 저장소 활성화
                settings.useWideViewPort = true // 뷰포트 설정
                settings.loadWithOverviewMode = true // 전체 뷰 모드 설정
            }
            binding.loginWebView.loadUrl(ServerUrlManager.serverUrl + "/oauth2/authorization/naver")
        }

        binding.testButton.setOnClickListener {
            //startActivity(Intent(this, PersonalInfoActivity::class.java))
            startActivity(Intent(this, PersonalInfoActivity::class.java))
        }


    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkUserStatus() {
        val request = Request.Builder()
            .url("${ServerUrlManager.serverUrl}/me/status")
            .addHeader("Authorization", "Bearer ${TokenManager.accessToken}")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread { showToast("Failed to check user status") }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread { showToast("Failed to check user status: ${response.code}") }
                    return
                }

                response.body?.string()?.let { responseBody ->
                    val status = JSONObject(responseBody).getString("status")
                    runOnUiThread {
                        when (status) {
                            "login" -> {
                                println("User have to LOGIN")
                                startActivity(
                                    Intent(
                                        this@LoginActivity2,
                                        ViewPagerActivity::class.java
                                    )
                                )
                            }
                            "join" -> {
                                println("User have to JOIN")
                                startActivity(
                                    Intent(
                                        this@LoginActivity2,
                                        PersonalInfoActivity::class.java
                                    )
                                )
                            }
                            else -> {
                                showToast("Unknown status")
                            }
                        }
                    }
                }
            }

        })
    }
}
