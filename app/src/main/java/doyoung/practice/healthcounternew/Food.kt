package doyoung.practice.healthcounternew

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food")
data class Food(
    val menu: String,
    var calorie: Int,
    val kind: String?,
    @PrimaryKey(autoGenerate = true)val id: Int =0,
)
