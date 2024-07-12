class GlobalData private constructor() {
    companion object {
        private var instance: GlobalData? = null
        private var globalCalorie: Float = 0.0f
        private var globalGoal: Float = 0.0f
        private var globalCalorieNow: Float = 0.0f

        fun getInstance(): GlobalData {
            if (instance == null) {
                instance = GlobalData()
            }
            return instance!!
        }
    }

    fun getGlobalCalorie(): Float {
        return globalCalorie
    }

    fun setGlobalCalorie(globalInt: Float) {
        globalCalorie = globalInt
    }

    fun getGlobalCalorieNow(): Float {
        return globalCalorieNow
    }

    fun setGlobalCalorieNow(globalInt: Float) {
        globalCalorieNow = globalInt
    }

    fun getGlobalGoal(): Float {
        return globalGoal
    }

    fun setGlobalGoal(value: Float) {
        globalGoal = value
    }

    fun getGlobalBurnedByExerciseCalorie(exerciseTime: Int): Float {
        val kcalPerMinute = 37 // 5분당 37kcal 소비
        return (exerciseTime / 5) * kcalPerMinute.toFloat()
    }
}
