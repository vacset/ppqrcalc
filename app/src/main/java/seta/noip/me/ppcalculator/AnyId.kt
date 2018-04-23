package seta.noip.me.ppcalculator

import java.util.*

class AnyId {
    var idType : String? = null
    var idValue : String? = null
    var aliasName : String? = null

    fun mask(): String {
        if (null == idType || null == idValue) {
            return ""
        }

        return when(idType) {
            PromptPayQR.BOT_ID_MERCHANT_PHONE_NUMBER
                -> idValue?.replaceFirst("(...)(...)(....)".toRegex(), "$1-XXX-$3") ?: ""
            PromptPayQR.BOT_ID_MERCHANT_TAX_ID
                -> idValue?.replaceFirst("(.)(....)(.....)(...)".toRegex(), "$1-$2-XXXXX-$4") ?: ""
            else
                -> {
                idValue?.let {
                    val n = it.length - 4
                    val chars = CharArray(n)
                    Arrays.fill(chars, 'X')
                    String(chars) + it.substring(it.length - 4)
                } ?: ""
            }
        }

    }
}