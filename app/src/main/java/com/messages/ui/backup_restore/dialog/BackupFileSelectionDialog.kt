package com.messages.ui.backup_restore.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams
import android.view.Window
import android.view.WindowManager
import com.messages.R
import com.messages.databinding.DialogBackupFileSelectionBinding
import com.messages.ui.backup_restore.adapter.BackupFileAdapter
import java.io.File

class BackupFileSelectionDialog(
    context: Context,
    private val backupFiles: List<File>,
    private var onClickFile: (File) -> Unit
) : Dialog(context, R.style.CustomDialog) {

    private val dialogBinding: DialogBackupFileSelectionBinding =
        DialogBackupFileSelectionBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            attributes.gravity = Gravity.BOTTOM
            setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply {
                dimAmount = 0.6f // Your desired dim amount
                flags = flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            }
        }
        setContentView(dialogBinding.root)
        initView()
    }

    private fun initView() {
        dialogBinding.apply {
            rcvBackupFiles.adapter =
                BackupFileAdapter(backupFile = backupFiles) {
                    onClickFile.invoke(it)
                    dismiss()
                }
        }
    }
}