package com.messages.data.models

sealed class BackupProgress(val running: Boolean = false, val indeterminate: Boolean = true) {
        class Idle : BackupProgress()
        class Parsing : BackupProgress(true)
        class Running(val max: Int, val count: Int) : BackupProgress(true, false)
        class Saving : BackupProgress(true)
        class Syncing : BackupProgress(true)
        class Finished : BackupProgress(true, false)
        class Saved(val backupTime:Long) : BackupProgress(true, false)
        class ErrorOnSave : BackupProgress(true, false)
    }
