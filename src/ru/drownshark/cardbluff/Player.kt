package ru.drownshark.cardbluff

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.ObjectInputStream
import java.lang.Exception
import java.net.Socket

class Player(private var id: Int? = null, hand: MutableList<Card> = mutableListOf()) : MessageListener() {
    override fun onMessage(message: Message) {
        println("${message.author}'th: " + message.text)
    }

    override fun onDisconnect(id : Int?) {
        println("> You are player #${this.id}: now disconnected from server")
    }

    fun move() {

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
                postman.sendMessage(Message(playerInput, id!!))
                playerInput = input.readLine()
            }
            catch (e : IOException) {
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