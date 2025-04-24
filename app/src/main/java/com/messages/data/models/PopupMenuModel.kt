package com.messages.data.models

import androidx.annotation.Keep

@Keep
data class PopupMenuModel(val menuId:Int,val menuName: String, val menuIcon: Int = 0)