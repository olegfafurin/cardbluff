package ru.drownshark.cardbluff

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.ObjectInputStream
import java.lang.Exception
import java.net.Socket

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

class Player(private var id: Int? = null, hand: MutableList<Card> = mutableListOf(), status: Int = 0) :
    MessageListener() {
    override fun onMessage(message: Message) {
        if (message.author == null) println(message.text)
        else if (message.author == -1) println("Server" + message.text)
        else println("Player #${message.author}': " + message.text)
    }

    override fun onDisconnect(id: Int?) {
        println("> You are player #${this.id}: now disconnected from server")
    }

    fun move() {

    }

    fun parse(s: String): Message {
        val args = s.split(" ") as MutableList<String>
        if (args[0].first() == '/') {
            val command = args[0]
            for (i in args.indices) {
                if (args[i].length == 1) {
                    args[i] = valueMap.getValue(args[i])
                }
            }
            return Message(args.drop(1).joinToString(" ").toUpperCase(), id!!, command)
        }
        return Message(s)
    }

    fun start() {
        val host = "127.0.0.1"
        val port = 1239
        val socketToServer = Socket(host, port)
        val input = BufferedReader(InputStreamReader(System.`in`))
        val m = ObjectInputStream(socketToServer.getInputStream()).readObject() as Message
        id = m.author
        println("> You are player #$id: now connected")

        val postman = StreamWorker(socketToServer.getInputStream(), socketToServer.getOutputStream()).apply {
            addListener(this@Player)
            start()
        }
        var playerInput = input.readLine()
        while (playerInput != null) {
            try {
                postman.sendMessage(parse(playerInput))
                playerInput = input.readLine()
            } catch (e: IOException) {
                println(e.message)
            }
        }
        postman.close()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Player().run { start() }
        }
    }

}