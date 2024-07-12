package doyoung.practice.healthcounternew

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.core.view.isVisible
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.contracts.contract
import kotlin.coroutines.coroutineContext

class LoginWebViewClient(
    private val progressBar: ProgressBar,
    private val webView: WebView,
    private val loginCallback: (String?, String?) -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        progressBar.isVisible = false

        url?.let {
            println("url: $url")
            if (it.startsWith("${ServerUrlManager.serverUrl}/login/oauth2/code/naver")) {
                val cookieManager = CookieManager.getInstance()
                val cookies = cookieManager.getCookie(it)
                var accessToken: String? = null
                var refreshToken: String? = null

                println("Cookies: $cookies")

                cookies?.split(";")?.forEach { cookie ->
                    val trimmedCookie = cookie.trim()
                    if (trimmedCookie.startsWith("access=")) {
                        accessToken = trimmedCookie.substringAfter("access=")
                    }
                    if (trimmedCookie.startsWith("refresh=")) {
                        refreshToken = trimmedCookie.substringAfter("refresh=")
                    }
                }

                println("Access-Token: $accessToken")
                println("Refresh-Token: $refreshToken")
                loginCallback(accessToken, refreshToken)
            }
        }


        // 쿠키 삭제를 통한 원활한 로그아웃, 재로그인 유도
        //clearCookiesAndCache(context)
    }


    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        progressBar.isVisible = true
        webView.isVisible = true
    }

    /*
    private fun clearCookiesAndCache(context: Context) {
        // Clear cookies
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()

        // Clear cache
        WebView(context).apply {
            clearCache(true)
            clearHistory()
        }
    }*/
}
