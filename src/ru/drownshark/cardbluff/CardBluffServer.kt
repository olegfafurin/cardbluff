package ru.drownshark.cardbluff

import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.SocketException
import kotlin.coroutines.jvm.*

class CardBluffServer(var connect: ServerSocket) : MessageListener() {

    private var messages: MutableList<Message> = mutableListOf()
    private var postmans: MutableMap<Int, StreamWorker> = mutableMapOf()
    private var games: MutableSet<Game> = mutableSetOf()
    private var awaitingPlayers : MutableSet<Int> = mutableSetOf()

    private val postmansLock = Any()

    override fun onMessage(message: Message) {
        println("${message.author}'th: " + message.text)
        messages.add(message)
        synchronized(postmansLock) {
            val toRemove : MutableList<Int> = mutableListOf()
            for (i in postmans.keys) {
                if (i != message.author) {
                    try {
                        postmans[i]?.sendMessage(message)
                    } catch (e: SocketException) {
                        toRemove.add(i)
                        println("> Player #$i is not available anymore, removed from pool")
                    }
                }
            }
            for (i in toRemove) postmans.remove(i)
        }
    }

    override fun onDisconnect(id: Int?) {
        println("$id: Some player disconnected from server")
    }


    fun start() {
        while (true) {
            val client = connect.accept()
            println(">> Player #$counter connected")
            ObjectOutputStream(client.getOutputStream()).run { writeObject(Message("", counter)) }
            val postman = StreamWorker(client.getInputStream(), client.getOutputStream())
            postman.addListener(this)
            postman.start()
            if (awaitingPlayers.isNotEmpty()) {
                val newGame = Game(counter, awaitingPlayers.first())
                GlobalScope.launch
            }
            synchronized(postmansLock) {
                postmans[counter++] = postman
                for (message in messages) postman.sendMessage(message)
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            CardBluffServer(ServerSocket(1239)).run { start() }
        }

        @JvmStatic
        var counter: Int = 0
    }

    override fun onException(e: Exception) {
        println()
        super.onException(e)
    }
}