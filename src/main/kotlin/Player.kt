package ru.drownshark.cardbluff

import java.io.*
import java.net.Socket
import kotlin.Exception

private val STATUS_WAITING = 0
private val STATUS_GAME_IN_PROGRESS = 1

private val valueMap: Map<String, String> = mapOf(
    "A" to "ACE", "a" to "ACE",
    "2" to "TWO",
    "3" to "THREE",
    "4" to "FOUR",
    "5" to "FIVE",
    "6" to "SIX",
    "7" to "SEVEN",
    "8" to "EIGHT",
    "9" to "NINE",
    "10" to "TEN", "0" to "TEN",
    "J" to "JACK", "j" to "JACK",
    "Q" to "QUEEN", "q" to "QUEEN",
    "K" to "KING", "k" to "KING",
    "♥" to "HEARTS",
    "♦" to "DIAMONDS",
    "♠" to "SPADES",
    "♣" to "CLUBS"
)

class Player(var id: Int = -1, private val postman: StreamWorker? = null, val nickname : String, var nCards: Int = 0) : // postman is StreamWorker to send messages TO current player.
    MessageListener() {
    override fun onMessage(message: Message) {
        if (message.author == null) println(message.text)
        else if (message.author == -1) println("Server" + message.text)
        else println("Player #${message.author}': " + message.text)
    }

    override fun onDisconnect(id: Int?) {
        println("> You are player #${this.id}: now disconnected from server")
    }

    fun sendIncomingMessage(msg: Message) {
        postman?.sendMessage(msg)
    }

    fun stopPostman() {
        postman?.input?.close()
    }
}