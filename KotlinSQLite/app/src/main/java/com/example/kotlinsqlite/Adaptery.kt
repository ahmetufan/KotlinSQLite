package com.example.kotlinsqlite

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinsqlite.databinding.RowBinding

class Adaptery(val artList:ArrayList<Art>): RecyclerView.Adapter<Adaptery.Holder>() {

    class Holder(val binding:RowBinding) : RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding=RowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.rowText.text=artList.get(position).name

        holder.itemView.setOnClickListener {
            val intent= Intent(holder.itemView.context,ArtActivity::class.java)
            intent.putExtra("info","old")
            intent.putExtra("id",artList.get(position).id)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return artList.size
    }
}