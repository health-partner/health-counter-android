package doyoung.practice.healthcounternew

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import doyoung.practice.healthcounternew.databinding.ItemLodedexerciseBinding

class LoadedRoutineAdapter(
    var list: MutableList<ExerciseRecycle>
) : RecyclerView.Adapter<LoadedRoutineAdapter.LoadedRoutineViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoadedRoutineViewHolder {
        val inflater =
            parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val binding = ItemLodedexerciseBinding.inflate(inflater, parent, false)
        return LoadedRoutineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LoadedRoutineViewHolder, position: Int) {
        val exercise = list.get(position)
        holder.bindName(exercise)
    }

    override fun getItemCount() = list.size

    var onItemClick: ((Int) -> Unit)? = null

    inner class LoadedRoutineViewHolder(val binding: ItemLodedexerciseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // 각 아이템 클릭 시에 대한 처리
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if(position!= RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(list[position].id)
                }
            }
        }

        fun bindName(routineName: ExerciseRecycle) {
            binding.apply {
                loadedExercise.text = routineName.name
            }
        }
    }

    // 추가된 메서드
    fun setData(data: List<ExerciseRecycle>?) {
        list.clear()
        data?.let {
            list.addAll(it)
        }
        notifyDataSetChanged()
    }
}