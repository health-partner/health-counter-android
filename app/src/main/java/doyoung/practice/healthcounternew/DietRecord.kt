package doyoung.practice.healthcounternew

data class DietRecord(
    val totalCalorie: Int,
    val mealTimeCalorieMap: Map<String, Int>
)

