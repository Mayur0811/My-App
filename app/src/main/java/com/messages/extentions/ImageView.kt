package com.messages.extentions

import android.net.Uri
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy


fun ImageView.loadImageWithOutCache(path: String? = null, uri: Uri? = null, plashHolder: Int = 0) {
    Glide.with(this).load(path ?: uri)
        .diskCacheStrategy(DiskCacheStrategy.NONE)
        .skipMemoryCache(true)
        .error(plashHolder)
        .placeholder(plashHolder).into(this)
}

fun ImageView.loadImage(path: String? = null, uri: Uri? = null, plashHolder: Int = 0) {
    Glide.with(this).load(path ?: uri)
        .error(plashHolder)
        .placeholder(plashHolder).into(this)
}

fun ImageView.loadImageDrawable(id: Int) {
    setImageDrawable(ContextCompat.getDrawable(this.context, id))
}