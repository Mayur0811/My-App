package com.messages.common.common_utils

import java.io.UnsupportedEncodingException

object CharacterSets {
    /**
     * IANA assigned MIB enum numbers.
     *
     * From wap-230-wsp-20010705-a.pdf
     * Any-charset = <Octet 128>
     * Equivalent to the special RFC2616 charset value "*"
    </Octet> */
    const val ANY_CHARSET: Int = 0x00
    const val US_ASCII: Int = 0x03
    const val ISO_8859_1: Int = 0x04
    const val ISO_8859_2: Int = 0x05
    const val ISO_8859_3: Int = 0x06
    const val ISO_8859_4: Int = 0x07
    const val ISO_8859_5: Int = 0x08
    const val ISO_8859_6: Int = 0x09
    const val ISO_8859_7: Int = 0x0A
    const val ISO_8859_8: Int = 0x0B
    const val ISO_8859_9: Int = 0x0C
    const val SHIFT_JIS: Int = 0x11
    const val UTF_8: Int = 0x6A
    const val BIG5: Int = 0x07EA
    const val UCS2: Int = 0x03E8
    const val UTF_16: Int = 0x03F7

    /**
     * If the encoding of given data is unsupported, use UTF_8 to decode it.
     */
    const val DEFAULT_CHARSET: Int = UTF_8

    /**
     * Array of MIB enum numbers.
     */
    private val MIB_ENUM_NUMBERS = intArrayOf(
        ANY_CHARSET,
        US_ASCII,
        ISO_8859_1,
        ISO_8859_2,
        ISO_8859_3,
        ISO_8859_4,
        ISO_8859_5,
        ISO_8859_6,
        ISO_8859_7,
        ISO_8859_8,
        ISO_8859_9,
        SHIFT_JIS,
        UTF_8,
        BIG5,
        UCS2,
        UTF_16,
    )

    /**
     * The Well-known-charset Mime name.
     */
    const val MIME_NAME_ANY_CHARSET: String = "*"
    const val MIME_NAME_US_ASCII: String = "us-ascii"
    const val MIME_NAME_ISO_8859_1: String = "iso-8859-1"
    const val MIME_NAME_ISO_8859_2: String = "iso-8859-2"
    const val MIME_NAME_ISO_8859_3: String = "iso-8859-3"
    const val MIME_NAME_ISO_8859_4: String = "iso-8859-4"
    const val MIME_NAME_ISO_8859_5: String = "iso-8859-5"
    const val MIME_NAME_ISO_8859_6: String = "iso-8859-6"
    const val MIME_NAME_ISO_8859_7: String = "iso-8859-7"
    const val MIME_NAME_ISO_8859_8: String = "iso-8859-8"
    const val MIME_NAME_ISO_8859_9: String = "iso-8859-9"
    const val MIME_NAME_SHIFT_JIS: String = "shift_JIS"
    const val MIME_NAME_UTF_8: String = "utf-8"
    const val MIME_NAME_BIG5: String = "big5"
    const val MIME_NAME_UCS2: String = "iso-10646-ucs-2"
    const val MIME_NAME_UTF_16: String = "utf-16"

    const val DEFAULT_CHARSET_NAME: String = MIME_NAME_UTF_8

    /**
     * Array of the names of character sets.
     */
    private val MIME_NAMES = arrayOf(
        MIME_NAME_ANY_CHARSET,
        MIME_NAME_US_ASCII,
        MIME_NAME_ISO_8859_1,
        MIME_NAME_ISO_8859_2,
        MIME_NAME_ISO_8859_3,
        MIME_NAME_ISO_8859_4,
        MIME_NAME_ISO_8859_5,
        MIME_NAME_ISO_8859_6,
        MIME_NAME_ISO_8859_7,
        MIME_NAME_ISO_8859_8,
        MIME_NAME_ISO_8859_9,
        MIME_NAME_SHIFT_JIS,
        MIME_NAME_UTF_8,
        MIME_NAME_BIG5,
        MIME_NAME_UCS2,
        MIME_NAME_UTF_16,
    )

    // Create the HashMaps.
    private val MIB_ENUM_TO_NAME_MAP = HashMap<Int, String>()
    private val NAME_TO_MIB_ENUM_MAP = HashMap<String, Int>()

    init {
        assert(MIB_ENUM_NUMBERS.size == MIME_NAMES.size)
        val count = MIB_ENUM_NUMBERS.size - 1
        for (i in 0..count) {
            MIB_ENUM_TO_NAME_MAP[MIB_ENUM_NUMBERS[i]] = MIME_NAMES[i]
            NAME_TO_MIB_ENUM_MAP[MIME_NAMES[i]] = MIB_ENUM_NUMBERS[i]
        }
    }

    /**
     * Map an MIBEnum number to the name of the charset which this number
     * is assigned to by IANA.
     *
     * @param mibEnumValue An IANA assigned MIBEnum number.
     * @return The name string of the charset.
     * @throws UnsupportedEncodingException
     */
    @Throws(UnsupportedEncodingException::class)
    fun getMimeName(mibEnumValue: Int): String {
        val name = MIB_ENUM_TO_NAME_MAP[mibEnumValue] ?: throw UnsupportedEncodingException()
        return name
    }

    /**
     * Map a well-known charset name to its assigned MIBEnum number.
     *
     * @param mimeName The charset name.
     * @return The MIBEnum number assigned by IANA for this charset.
     * @throws UnsupportedEncodingException
     */
    @Throws(UnsupportedEncodingException::class)
    fun getMibEnumValue(mimeName: String?): Int {
        if (null == mimeName) {
            return -1
        }
        val mibEnumValue = NAME_TO_MIB_ENUM_MAP[mimeName] ?: throw UnsupportedEncodingException()
        return mibEnumValue
    }
}
