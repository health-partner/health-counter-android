package doyoung.practice.healthcounternew

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import doyoung.practice.healthcounternew.databinding.ItemExerciseBinding

class ExerciseAdapter(
    private var list: MutableList<Exercise>,
    private val itemClickListener: ItemClickListener? = null,
): RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val binding = ItemExerciseBinding.inflate(inflater, parent, false)
        return ExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = list[position]
        holder.bind(exercise)
        holder.itemView.setOnClickListener { itemClickListener?.onClick(exercise) }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ExerciseViewHolder(private val binding: ItemExerciseBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(exercise: Exercise) {
            binding.apply{
                exerciseNameTextView.text = exercise.name
            }
        }
    }

    interface ItemClickListener {
        fun onClick(exercise: Exercise)
    }

    fun getList(): MutableList<Exercise> {
        return list
    }

    fun setList(newList: MutableList<Exercise>) {
        list = newList
    }
}