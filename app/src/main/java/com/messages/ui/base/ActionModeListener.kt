package com.messages.ui.base

import androidx.appcompat.view.ActionMode


abstract class ActionModeListener : ActionMode.Callback {
    var isSelectable = false
}
