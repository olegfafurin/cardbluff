package ru.drownshark.cardbluff

import java.io.Serializable

/**
 * Created by imd on 09/03/2019
 */

data class Message(var text : String = "", var author : Int? = null, var command : String = "") : Serializable {
}