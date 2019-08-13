package ru.drownshark.cardbluff

import java.io.*
import java.lang.IndexOutOfBoundsException
import java.net.Socket
import java.net.SocketException
import kotlin.Exception

private const val STATUS_WAITING = 0
private const val STATUS_GAME_IN_PROGRESS = 1

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

class CardBluffClient(
    var id: Int = -1,
    private var username: String = "<undefined>"
) :
    MessageListener() {

    private var state: Int = STATUS_WAITING
    private var authorised: Boolean = false
    var playerInput = ""
    var postman: StreamWorker? = null
    var input: BufferedReader? = null
    var socketToServer: Socket? = null

    private val mainThread: Thread = Thread.currentThread()

    override fun onMessage(message: Message) {
        if (message.author == null) println(message.text)
        else if (message.author == -1) println("Server" + message.text)
        else println("@${message.authorNickname}: " + message.text)
        if (message.author == -1 && message.command == "/exit") {
            state = STATUS_WAITING
            mainThread.interrupt()
        }
    }

    override fun onDisconnect(id: Int?) {
        println("> You are @$username: now disconnected from server")
    }

    private fun parse(s: String): Message {
        try {
            val args = s.split(" ") as MutableList<String>
            if (args[0].first() == '/') {
                val command = args[0]
                for (i in args.indices) {
                    if (args[i].length == 1) {
                        args[i] = valueMap.getValue(args[i])
                    }
                }
                return Message(args.drop(1).joinToString(" ").toUpperCase(), id, username, command)
            }
            return Message(s, id, username)
        } catch (e: IndexOutOfBoundsException) {
            println(e.message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Message(s, id, username)
    }


    override fun onException(e: Exception) {
        super.onException(e)
        println("onException called")
    }

    private fun initialState() {
        while (true) {
            println("Type \"/start\" to begin the game, \"/help\" to read the rules")
            val command = readLine()
            if (command?.startsWith("/start") == true) {
                try {
                    val args = command.split(" ") as MutableList<String>
                    when (args.size) {
                        2 -> startGame("127.0.0.1", args[2].toInt())
                        3 -> startGame(args[1], args[2].toInt())
                        else -> startGame("127.0.0.1", 1239)
                    }
                } catch (e: Exception) {
                    println("Unexpected exception occurred!")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun getUsername(): String {
        print("Type your nickname: ")
        return readLine() ?: "<undefined>"
    }

    private fun getPasswd(): String {
        print("Password: ")
        return readLine() ?: "default"
    }

    fun startGame(host: String, port: Int) {
        state = STATUS_GAME_IN_PROGRESS
        try {
            socketToServer = Socket(host, port)
            input = BufferedReader(InputStreamReader(System.`in`))
            var m: Message
            if (!authorised) {
                username = getUsername()
                ObjectOutputStream(socketToServer?.getOutputStream()).run {
                    writeObject(
                        Message(
                            username,
                            -1,
                            "<undefined>"
                        )
                    )
                } // send username

                m = ObjectInputStream(socketToServer?.getInputStream()).readObject() as Message // receive an answer (request for password?)
                println(m.text)
                if (m.command == "/auth") {
                    ObjectOutputStream(socketToServer?.getOutputStream()).run {
                        writeObject(
                            Message(
                                getPasswd(),
                                -1,
                                "<undefined>"
                            )
                        )
                    } // send password
                } else {
                    println("something is wrong...")
                    return
                }
                m = ObjectInputStream(socketToServer?.getInputStream()).readObject() as Message // recieve an ID
                println("> You are player #${m.author} a.k.a. @$username: now connected")
                authorised = true
            } else {
                ObjectOutputStream(socketToServer?.getOutputStream()).run {
                    writeObject(
                        Message(
                            username,
                            -1,
                            "<undefined>"
                        )
                    )
                }
                m = ObjectInputStream(socketToServer?.getInputStream()).readObject() as Message // recieve an ID
                println("> You are player #${m.author} a.k.a. @$username: now connected")
            }
            postman = StreamWorker(
                socketToServer!!.getInputStream(),
                socketToServer!!.getOutputStream()
            ) // postman to deliver messages to this client.
            this.id = m.author!!
            postman!!.apply {
                addListener(this@CardBluffClient)
                start()
            }

            while (playerInput != "/exit" || state == STATUS_WAITING) { // TODO: Add auto exit on disconnect
                try {
                    playerInput = input!!.readLine()
                    if (state == STATUS_WAITING) break
                    postman!!.sendMessage(parse(playerInput))
                } catch (e: Exception) {
                    println(e.message)
                    e.printStackTrace()
                }
            }
            postman!!.close()
            socketToServer?.close()
        } catch (e: SocketException) {
            e.printStackTrace()
            println("Can't connect to the server given. Check your connection and try again.")
        }
    }

    override fun onGameEnded() {

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            printLogo()
            val client = CardBluffClient()
            client.initialState()
        }

        private fun printLogo() = File("newlogo2.txt").forEachLine { println(it) }
    }

}