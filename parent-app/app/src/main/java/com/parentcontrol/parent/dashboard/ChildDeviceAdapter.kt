package com.parentcontrol.parent.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.parentcontrol.parent.databinding.ItemChildBinding
import com.parentcontrol.parent.model.ChildDevice

class ChildDeviceAdapter(
    private val onClickManage: (ChildDevice) -> Unit
) : ListAdapter<ChildDevice, ChildDeviceAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemChildBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(child: ChildDevice) {
            binding.tvDeviceModel.text = child.deviceModel
            binding.tvChildId.text = "ID: ${child.childId.take(8)}..."
            binding.btnManage.setOnClickListener { onClickManage(child) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemChildBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ChildDevice>() {
            override fun areItemsTheSame(a: ChildDevice, b: ChildDevice) = a.childId == b.childId
            override fun areContentsTheSame(a: ChildDevice, b: ChildDevice) = a == b
        }
    }
}
