package doyoung.practice.healthcounternew

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import doyoung.practice.healthcounternew.databinding.ItemLodedexerciseBinding

class LoadedExerciseAdapter(
    var list: MutableList<Exercise>,
) : RecyclerView.Adapter<LoadedExerciseAdapter.LoadedExerciseViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoadedExerciseViewHolder {
        val inflater =
            parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val binding = ItemLodedexerciseBinding.inflate(inflater, parent, false)
        return LoadedExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LoadedExerciseViewHolder, position: Int) {
        val exercise = list.get(position)
        holder.bindName(exercise)
    }

    override fun getItemCount() = list.size

    class LoadedExerciseViewHolder(val binding: ItemLodedexerciseBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindName(exerciseName: Exercise) {
            binding.apply {
                loadedExercise.text = exerciseName.name
            }
        }
    }

    // 추가된 메서드
    fun setData(data: List<Exercise>?) {
        list.clear()
        data?.let {
            list.addAll(it)
        }
        notifyDataSetChanged()
    }
}