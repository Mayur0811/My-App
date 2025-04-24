package com.messages.ui.backup_restore.ui

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.messages.R
import com.messages.common.PermissionManager
import com.messages.common.mainScope
import com.messages.data.models.BackupProgress
import com.messages.data.repository.MessageRepository
import com.messages.databinding.ActivityBackupRestoreBinding
import com.messages.extentions.formatDateLastBackup
import com.messages.extentions.getBackupDir
import com.messages.extentions.getStringValue
import com.messages.extentions.toast
import com.messages.ui.backup_restore.dialog.BackupFileSelectionDialog
import com.messages.ui.base.BaseActivity
import com.messages.utils.AppLogger
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class BackupRestoreActivity : BaseActivity() {

    private val binding by lazy { ActivityBackupRestoreBinding.inflate(layoutInflater) }

    private var isBackup = true

    @Inject
    lateinit var messageRepository: MessageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initToolbar()
        initData()
        initAction()
        initObserver()
    }

    private fun initObserver() {
        lifecycleScope.launch {
            messageRepository.backupProgress.observe(this@BackupRestoreActivity) {
                mainScope.launch {
                    when (it) {
                        is BackupProgress.Running -> binding.backupProgress.isVisible = true
                        is BackupProgress.Finished -> binding.backupProgress.isVisible = false
                        is BackupProgress.Idle -> binding.backupProgress.isVisible = false
                        is BackupProgress.Saved -> {
                            AppLogger.d("1234","backupTime -> ${it.backupTime}")
                            appPreferences.lastExportDate = it.backupTime
                            binding.backupMessage.getBinding().customViewSubtitle.text = getString(
                                R.string.last_backup_time,
                                it.backupTime.formatDateLastBackup(appPreferences.use24Hr)
                            )
                        }
                        else -> {}
                    }
                }

            }
        }

        lifecycleScope.launch {
            messageRepository.restoreProgress.observe(this@BackupRestoreActivity) {
                mainScope.launch {
                    when (it) {
                        is BackupProgress.Running -> binding.restoreProgress.isVisible = true
                        is BackupProgress.Finished -> binding.restoreProgress.isVisible = false
                        is BackupProgress.Idle -> binding.restoreProgress.isVisible = false
                        else -> {}
                    }
                }

            }
        }
    }

    private fun initAction() {
        binding.apply {
            backupMessage.setOnSafeClickListener {
                isBackup = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    messageRepository.performBackup()
                } else {
                    handleStoragePermission {
                        messageRepository.performBackup()
                    }
                }
            }

            restoreMessage.setOnSafeClickListener {
                isBackup = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    openBackupFileSelectionDialog()
                } else {
                    handleStoragePermission {
                        openBackupFileSelectionDialog()
                    }
                }
            }
        }
    }

    private fun initToolbar() {
        binding.toolbar.getBinding().apply {
            tvToolbarTitle.text = getStringValue(R.string.backup_restore)
            ivToolbarBack.setOnSafeClickListener { finish() }
        }
    }

    private fun initData() {
        binding.apply {
            if (appPreferences.lastExportDate == 0L) {
                backupMessage.getBinding().customViewSubtitle.text =
                    getString(R.string.no_backup_available)
            } else {
                backupMessage.getBinding().customViewSubtitle.text = getString(
                    R.string.last_backup_time,
                    appPreferences.lastExportDate.formatDateLastBackup(appPreferences.use24Hr)
                )
            }
        }
    }

    private fun openBackupFileSelectionDialog() {
        val files = File(getBackupDir()).listFiles()?.toList()
        if (files?.isNotEmpty() == true) {
            BackupFileSelectionDialog(this, files.sortedByDescending { it.nameWithoutExtension }) {
                messageRepository.performRestore(Uri.fromFile(it))
            }.show()
        } else {
            toast(msg = getStringValue(R.string.not_found_any_backup_file))
        }
    }

    override fun onGetPermission(permission: Int, isGranted: Boolean) {
        if (permission == PermissionManager.PERMISSION_READ_STORAGE) {
            if (isBackup) {
                messageRepository.performBackup()
            } else {
                openBackupFileSelectionDialog()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndAskDefaultApp()
    }

}