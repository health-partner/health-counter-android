package doyoung.practice.healthcounternew

import androidx.room.*

@Dao
interface FoodDao {
    @Query("SELECT * from food ORDER BY id DESC")
    fun getAll(): List<Food>

    @Delete
    fun delete(food: Food)

    @Update
    fun update(food: Food)

    @Insert
    fun insert(food: Food)

    @Query("SELECT SUM(calorie) FROM food WHERE kind = '아침'")
    fun getTotalCaloriesForBreakfast(): Int

    @Query("SELECT menu FROM food WHERE kind = '아침'")
    fun getBreakfastMenus(): List<String>

    // 4개씩 메뉴들을 묶어서 문장으로 반환합니다.
    fun getFormattedBreakfastMenus(): String {
        val menus = getBreakfastMenus()
        val result = StringBuilder()

        for (i in menus.indices step 4) {
            val endIndex = (i + 4).coerceAtMost(menus.size) // 네 개까지 묶어서 처리
            val menuGroup = menus.subList(i, endIndex).joinToString(", ")
            result.append("$menuGroup\n")
        }

        return result.toString().trim()
    }

    @Query("SELECT SUM(calorie) FROM food WHERE kind = '점심'")
    fun getTotalCaloriesForLunch(): Int

    @Query("SELECT menu FROM food WHERE kind = '점심'")
    fun getLunchMenus(): List<String>

    // 4개씩 메뉴들을 묶어서 문장으로 반환합니다.
    fun getFormattedLunchMenus(): String {
        val menus = getLunchMenus()
        val result = StringBuilder()

        for (i in menus.indices step 4) {
            val endIndex = (i + 4).coerceAtMost(menus.size) // 네 개까지 묶어서 처리
            val menuGroup = menus.subList(i, endIndex).joinToString(", ")
            result.append("$menuGroup\n")
        }

        return result.toString().trim()
    }


    @Query("SELECT SUM(calorie) FROM food WHERE kind = '저녁'")
    fun getTotalCaloriesForDinner(): Int

    @Query("SELECT menu FROM food WHERE kind = '저녁'")
    fun getDinnerMenus(): List<String>

    // 4개씩 메뉴들을 묶어서 문장으로 반환합니다.
    fun getFormattedDinnerMenus(): String {
        val menus = getDinnerMenus()
        val result = StringBuilder()

        for (i in menus.indices step 4) {
            val endIndex = (i + 4).coerceAtMost(menus.size) // 네 개까지 묶어서 처리
            val menuGroup = menus.subList(i, endIndex).joinToString(", ")
            result.append("$menuGroup\n")
        }

        return result.toString().trim()
    }

    @Query("DELETE FROM food")
    fun deleteAll()
}