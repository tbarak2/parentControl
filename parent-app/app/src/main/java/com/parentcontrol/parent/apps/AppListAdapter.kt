package com.parentcontrol.parent.apps

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.parentcontrol.parent.databinding.ItemAppBinding
import com.parentcontrol.parent.model.AppInfo

class AppListAdapter(
    private val onToggleBlock: (AppInfo, Boolean) -> Unit,
    private val onSetLimit: (AppInfo) -> Unit
) : ListAdapter<AppInfo, AppListAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(app: AppInfo) {
            binding.tvAppName.text = app.appName
            binding.tvPackageName.text = app.packageName
            val limit = app.dailyLimitMinutes
            binding.tvLimit.text = if (limit != null && limit > 0) "Limit: ${limit}m/day" else "No limit"
            binding.switchBlock.setOnCheckedChangeListener(null)
            binding.switchBlock.isChecked = app.isBlocked
            binding.switchBlock.setOnCheckedChangeListener { _, isChecked -> onToggleBlock(app, isChecked) }
            binding.btnSetLimit.setOnClickListener { onSetLimit(app) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(a: AppInfo, b: AppInfo) = a.packageName == b.packageName
            override fun areContentsTheSame(a: AppInfo, b: AppInfo) = a == b
        }
    }
}
