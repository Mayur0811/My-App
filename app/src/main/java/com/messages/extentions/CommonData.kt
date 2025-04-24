package com.messages.extentions

import android.content.Context
import com.messages.R


fun Context.getClipboardTypeData(position: Int): String {
    return when (position) {
        0 -> getStringValue(R.string.default_category)
        1 -> getStringValue(R.string.funny_category)
        2 -> getStringValue(R.string.love_category)
        3 -> getStringValue(R.string.wishes_category)
        4 -> getStringValue(R.string.sad_category)
        5 -> getStringValue(R.string.friend_category)
        6 -> getStringValue(R.string.birthday_category)
        7 -> getStringValue(R.string.life_category)
        8 -> getStringValue(R.string.motivation_category)
        9 -> getStringValue(R.string.savage_category)
        else -> getStringValue(R.string.default_category)
    }
}

fun Context.getClipboardData(position: Int): ArrayList<String> {
    return when (position) {
        0 -> arrayListOf(
            getStringValue(R.string.default_sentence_1),
            getStringValue(R.string.default_sentence_2),
            getStringValue(R.string.default_sentence_3),
            getStringValue(R.string.default_sentence_4),
            getStringValue(R.string.default_sentence_5),
            getStringValue(R.string.default_sentence_6),
            getStringValue(R.string.default_sentence_7),
            getStringValue(R.string.default_sentence_8),
            getStringValue(R.string.default_sentence_9),
            getStringValue(R.string.default_sentence_10)
        )

        1 -> arrayListOf(
            getStringValue(R.string.funny_sentence_1),
            getStringValue(R.string.funny_sentence_2),
            getStringValue(R.string.funny_sentence_3),
            getStringValue(R.string.funny_sentence_4),
            getStringValue(R.string.funny_sentence_5),
            getStringValue(R.string.funny_sentence_6),
            getStringValue(R.string.funny_sentence_7),
            getStringValue(R.string.funny_sentence_8),
            getStringValue(R.string.funny_sentence_9),
            getStringValue(R.string.funny_sentence_10)
        )

        2 -> arrayListOf(
            getStringValue(R.string.love_sentence_1),
            getStringValue(R.string.love_sentence_2),
            getStringValue(R.string.love_sentence_3),
            getStringValue(R.string.love_sentence_4),
            getStringValue(R.string.love_sentence_5),
            getStringValue(R.string.love_sentence_6),
            getStringValue(R.string.love_sentence_7),
            getStringValue(R.string.love_sentence_8),
            getStringValue(R.string.love_sentence_9),
            getStringValue(R.string.love_sentence_10)
        )

        3 -> arrayListOf(
            getStringValue(R.string.wishes_sentence_1),
            getStringValue(R.string.wishes_sentence_2),
            getStringValue(R.string.wishes_sentence_3),
            getStringValue(R.string.wishes_sentence_4),
            getStringValue(R.string.wishes_sentence_5),
            getStringValue(R.string.wishes_sentence_6),
            getStringValue(R.string.wishes_sentence_7),
            getStringValue(R.string.wishes_sentence_8),
            getStringValue(R.string.wishes_sentence_9),
            getStringValue(R.string.wishes_sentence_10)
        )

        4 -> arrayListOf(
            getStringValue(R.string.sad_sentence_1),
            getStringValue(R.string.sad_sentence_2),
            getStringValue(R.string.sad_sentence_3),
            getStringValue(R.string.sad_sentence_4),
            getStringValue(R.string.sad_sentence_5),
            getStringValue(R.string.sad_sentence_6),
            getStringValue(R.string.sad_sentence_7),
            getStringValue(R.string.sad_sentence_8),
            getStringValue(R.string.sad_sentence_9),
            getStringValue(R.string.sad_sentence_10)
        )

        5 -> arrayListOf(
            getStringValue(R.string.friends_sentence_1),
            getStringValue(R.string.friends_sentence_2),
            getStringValue(R.string.friends_sentence_3),
            getStringValue(R.string.friends_sentence_4),
            getStringValue(R.string.friends_sentence_5),
            getStringValue(R.string.friends_sentence_6),
            getStringValue(R.string.friends_sentence_7),
            getStringValue(R.string.friends_sentence_8),
            getStringValue(R.string.friends_sentence_9),
            getStringValue(R.string.friends_sentence_10)
        )

        6 -> arrayListOf(
            getStringValue(R.string.birthday_sentence_1),
            getStringValue(R.string.birthday_sentence_2),
            getStringValue(R.string.birthday_sentence_3),
            getStringValue(R.string.birthday_sentence_4),
            getStringValue(R.string.birthday_sentence_5),
            getStringValue(R.string.birthday_sentence_6),
            getStringValue(R.string.birthday_sentence_7),
            getStringValue(R.string.birthday_sentence_8),
            getStringValue(R.string.birthday_sentence_9),
            getStringValue(R.string.birthday_sentence_10)
        )

        7 -> arrayListOf(
            getStringValue(R.string.life_sentence_1),
            getStringValue(R.string.life_sentence_2),
            getStringValue(R.string.life_sentence_3),
            getStringValue(R.string.life_sentence_4),
            getStringValue(R.string.life_sentence_5),
            getStringValue(R.string.life_sentence_6),
            getStringValue(R.string.life_sentence_7),
            getStringValue(R.string.life_sentence_8),
            getStringValue(R.string.life_sentence_9),
            getStringValue(R.string.life_sentence_10)
        )

        8 -> arrayListOf(
            getStringValue(R.string.motivation_sentence_1),
            getStringValue(R.string.motivation_sentence_2),
            getStringValue(R.string.motivation_sentence_3),
            getStringValue(R.string.motivation_sentence_4),
            getStringValue(R.string.motivation_sentence_5),
            getStringValue(R.string.motivation_sentence_6),
            getStringValue(R.string.motivation_sentence_7),
            getStringValue(R.string.motivation_sentence_8),
            getStringValue(R.string.motivation_sentence_9),
            getStringValue(R.string.motivation_sentence_10)
        )

        9 -> arrayListOf(
            getStringValue(R.string.savage_sentence_1),
            getStringValue(R.string.savage_sentence_2),
            getStringValue(R.string.savage_sentence_3),
            getStringValue(R.string.savage_sentence_4),
            getStringValue(R.string.savage_sentence_5),
            getStringValue(R.string.savage_sentence_6),
            getStringValue(R.string.savage_sentence_7),
            getStringValue(R.string.savage_sentence_8),
            getStringValue(R.string.savage_sentence_9),
            getStringValue(R.string.savage_sentence_10)
        )

        else -> arrayListOf(
            getStringValue(R.string.default_sentence_1),
            getStringValue(R.string.default_sentence_2),
            getStringValue(R.string.default_sentence_3),
            getStringValue(R.string.default_sentence_4),
            getStringValue(R.string.default_sentence_5),
            getStringValue(R.string.default_sentence_6),
            getStringValue(R.string.default_sentence_7),
            getStringValue(R.string.default_sentence_8),
            getStringValue(R.string.default_sentence_9),
            getStringValue(R.string.default_sentence_10)
        )
    }
}