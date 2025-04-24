package com.messages.ui.backup_restore.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.messages.databinding.ItemBackupFilesBinding
import com.messages.utils.setOnSafeClickListener
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class BackupFileAdapter(
    private val backupFile: List<File>, private var onClickFile: (File) -> Unit
) : RecyclerView.Adapter<BackupFileAdapter.BackupFileViewHolder>() {

    val inputFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
    val outputFormat = SimpleDateFormat("MM/dd/yyyy, hh:mm:ss a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackupFileViewHolder {
        return BackupFileViewHolder(
            ItemBackupFilesBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return backupFile.size
    }

    override fun onBindViewHolder(holder: BackupFileViewHolder, position: Int) {
        holder.bind(backupFile[position])
    }

    inner class BackupFileViewHolder(val binding: ItemBackupFilesBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(file: File) {
            binding.apply {

                tvBackupFileTitle.text = file.name
                val time = file.nameWithoutExtension.split("-")[1]
                val date = inputFormat.parse(time)
                tvBackupFileDate.text = date?.let { outputFormat.format(it) }

                root.setOnSafeClickListener {
                    onClickFile.invoke(file)
                }
            }
        }
    }
}