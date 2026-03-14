package com.parentcontrol.parent.requests

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.parentcontrol.parent.databinding.ItemUnlockRequestBinding
import com.parentcontrol.parent.model.UnlockRequest

class UnlockRequestAdapter(
    private val onApprove: (UnlockRequest, Int) -> Unit,
    private val onDeny: (UnlockRequest) -> Unit
) : ListAdapter<UnlockRequest, UnlockRequestAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemUnlockRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(request: UnlockRequest) {
            binding.tvPackageName.text = request.packageName
            binding.tvRequestedAt.text = "Requested: ${request.requestedAt}"
            binding.btnApprove30.setOnClickListener { onApprove(request, 30) }
            binding.btnApprove60.setOnClickListener { onApprove(request, 60) }
            binding.btnDeny.setOnClickListener { onDeny(request) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemUnlockRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<UnlockRequest>() {
            override fun areItemsTheSame(a: UnlockRequest, b: UnlockRequest) = a.packageName == b.packageName
            override fun areContentsTheSame(a: UnlockRequest, b: UnlockRequest) = a == b
        }
    }
}
