package ru.drownshark.cardbluff

import java.io.Serializable

/**
 * Created by imd on 09/03/2019
 */

data class Message(var text : String = "", var author : Int? = null, var authorNickname: String = "SERVER", var command : String = "") : Serializable {
}