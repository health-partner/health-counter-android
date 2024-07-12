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
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import doyoung.practice.healthcounternew.databinding.FragmentHomeBinding
import doyoung.practice.healthcounternew.databinding.FragmentMypageBinding
import okhttp3.*
import java.io.IOException

class FragmentMypage : Fragment() {
    lateinit var binding: FragmentMypageBinding
    lateinit var ViewPagerActivity: ViewPagerActivity


    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is ViewPagerActivity) ViewPagerActivity = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMypageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바인딩 로직
        binding.logoutButton.setOnClickListener {
            showAlertDialog()
        }

        binding.editPersonalInfoTextView.setOnClickListener {
            val intent = Intent(context, EditPersonalInfoActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showAlertDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("정말로 로그아웃하시겠습니까?")
        builder.setMessage("사용자 맞춤 서비스를 제공할 수 없습니다.")

        // 루틴 설정 클릭
        builder.setPositiveButton("네") { dialog, which ->
            logoutFromServer()
        }

        builder.setNegativeButton("아니오") { dialog, which ->

        }

        builder.show()
    }

    // 토스트 메시지 형식
    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun logoutFromServer() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${ServerUrlManager.serverUrl}/logout")
            .addHeader("Authorization", "Bearer ${TokenManager.accessToken}")
            .addHeader("Refresh-Token", TokenManager.refreshToken ?: "")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                showToast("Failed to log out from server")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    showToast("Failed to log out from server: ${response.code}")
                    return
                }


                println("토큰 삭제 전 리프레쉬: ${TokenManager.refreshToken}, 액세스: ${TokenManager.accessToken}")
                // 토큰 삭제
                TokenManager.clearTokens(requireContext())
                println("토큰 삭제 후 리프레쉬: ${TokenManager.refreshToken}, 액세스: ${TokenManager.accessToken}")

                // 쿠키와 세션 삭제
                clearCookiesAndCache(context!!)

                // 로그아웃 후 로그인 화면으로 이동
                val intent = Intent(context, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        })
    }

    private fun clearCookiesAndCache(context: Context) {
        // Clear cookies
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()

        // Clear cache
        Handler(Looper.getMainLooper()).post {
            WebView(context).apply {
                clearCache(true)
                clearHistory()
            }
        }
    }
}