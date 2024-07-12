package doyoung.practice.healthcounternew

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import doyoung.practice.healthcounternew.databinding.ActivityViewPagerBinding

class ViewPagerActivity : AppCompatActivity() {

    private var bluetoothDeviceAddress: String? = null

    fun setBluetoothDeviceAddress(address: String) {
        this.bluetoothDeviceAddress = address
    }

    fun getBluetoothDeviceAddress(): String? {
        return bluetoothDeviceAddress
    }

    private lateinit var binding: ActivityViewPagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewPagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 운동, 식단, 홈페이지, 리포트, 마이페이지
        val list = listOf(FragmentExercise(), FragmentFood(), FragmentHome(), FragmentReport(), FragmentMypage())
        // 어댑터 생성
        val adapter = FragmentPagerAdapter(list, this)
        // 어댑터와 뷰 페이저의 연결
        binding.viewPager.adapter = adapter

        // 기본으로 보여줄 프래그먼트의 인덱스 설정 (세 번째 프래그먼트)
        val defaultPageIndex = 2
        binding.viewPager.setCurrentItem(defaultPageIndex, false) // false를 통해 애니메이션 없이 페이지 변경

        // 탭 레이아웃과 뷰 페이저의 연결
        val tabTitles = listOf("운동", "식단", "홈", "리포트", "내 정보")
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }
}

class FragmentPagerAdapter(
    private val fragmentList: List<Fragment>,
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount() = fragmentList.size
    override fun createFragment(position: Int) = fragmentList[position]
}

