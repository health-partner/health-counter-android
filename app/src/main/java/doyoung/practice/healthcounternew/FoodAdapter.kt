package doyoung.practice.healthcounternew

import android.content.Context
import android.text.Layout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import doyoung.practice.healthcounternew.databinding.ItemFoodBinding

import java.util.*

class FoodAdapter(
    private val list: MutableList<Food>,
    private val itemClickListener: ItemClickListen? = null,
    private val totalCaloriesTextView: TextView,
    private val activity: CalorieActivity
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    private val cntValueMap = mutableMapOf<Int, Int>() // 각 아이템의 cntValue를 저장하는 맵

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val inflater =
            parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val binding = ItemFoodBinding.inflate(inflater, parent, false)
        return FoodViewHolder(binding, itemClickListener, this, activity)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = list[position]
        holder.bind(food, cntValueMap[position] ?: 1, position) // position을 추가로 전달
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class FoodViewHolder(
        private val binding: ItemFoodBinding,
        private val itemClickListener: ItemClickListen?,
        private val adapter: FoodAdapter,
        private val activity: CalorieActivity
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentPosition: Int = -1 // 현재 아이템의 위치 저장

        init {
            binding.upButton.setOnClickListener {
                updateCntValue(1)
            }

            binding.downButton.setOnClickListener {
                updateCntValue(-1)
            }

            binding.root.setOnClickListener {
                itemClickListener?.onClick(currentPosition, getCntValue())
            }
        }

        fun bind(food: Food, cntValue: Int, position: Int) {
            currentPosition = position // 현재 아이템의 위치 저장
            binding.apply {
                foodNameTextView.text = food.menu
                loadedCalorie.text = "${food.calorie * cntValue} kcal"
                cntTextView.text = "${cntValue}인분"
            }

            // 아이템의 위치(인덱스)에 따른 cntValue 업데이트
            adapter.cntValueMap[position] = cntValue
        }

        private fun updateCntValue(change: Int) {
            val currentCntValue = adapter.cntValueMap[currentPosition] ?: 1
            val newCntValue = (currentCntValue + change).coerceIn(0, 5) // 0에서 5 사이로 제한
            binding.cntTextView.text = "${newCntValue}인분"
            adapter.cntValueMap[currentPosition] = newCntValue
            updateLoadedCalorie()
        }

        private fun getCntValue(): Int {
            return adapter.cntValueMap[currentPosition] ?: 1
        }

        private fun updateLoadedCalorie() {
            Log.d("FoodAdapter", "Updating loaded calorie for position: $currentPosition")
            val food = adapter.list[currentPosition]
            val itemCalories = food.calorie * getCntValue()
            binding.loadedCalorie.text = "${itemCalories} kcal"
            adapter.updateTotalCalories()

            adapter.list[currentPosition].calorie = itemCalories
            Thread {
                Log.d("FoodAdapter", "Updating food in the database: ${adapter.list[currentPosition]}")
                AppDatabase.getInstance(activity)?.foodDao()?.update(adapter.list[currentPosition])
            }.start()
        }
    }

    private fun updateTotalCalories() {
        val totalCalories = list.indices.sumOf { position ->
            (list[position].calorie * (cntValueMap[position] ?: 1))
        }
        totalCaloriesTextView.text = "한 끼 섭취량: $totalCalories kcal"
    }

    interface ItemClickListen {
        fun onClick(position: Int, cntValue: Int)
    }

    fun setData(newList: List<Food>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
        updateTotalCalories()
        Log.d("FoodAdapter", "setData called. Total items: ${list.size}")
    }

    fun getSelectedMenus(): List<String> {
        val selectedMenus = mutableListOf<String>()
        for (position in list.indices) {
            val menu = list[position].menu
            if (menu.lowercase() != "unclear") {
                selectedMenus.add(menu)
            }
        }
        return selectedMenus
    }

    fun getDietInfo(): List<Map<String, Any>> {
        val dietInfo = list.mapIndexed { index, food ->
            mapOf(
                "dish" to food.menu.toString(),
                "calorie" to (food.calorie * (cntValueMap[index] ?: 1))
            )
        }
        Log.d("DietInfo", dietInfo.toString()) // 로그에 dietInfo를 출력합니다.
        return dietInfo
    }

}
