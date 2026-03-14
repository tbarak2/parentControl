package com.parentcontrol.parent.usage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.parentcontrol.parent.databinding.ItemUsageBinding
import com.parentcontrol.parent.model.AppUsage

class UsageAdapter : ListAdapter<AppUsage, UsageAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemUsageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AppUsage) {
            binding.tvAppName.text = item.appName
            binding.tvPackageName.text = item.packageName
            val h = item.minutes / 60
            val m = item.minutes % 60
            binding.tvUsageTime.text = if (h > 0) "${h}h ${m}m" else "${m}m"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemUsageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AppUsage>() {
            override fun areItemsTheSame(a: AppUsage, b: AppUsage) = a.packageName == b.packageName
            override fun areContentsTheSame(a: AppUsage, b: AppUsage) = a == b
        }
    }
}
