package ru.drownshark.cardbluff

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.SocketException

class CardBluffServer(var connect: ServerSocket) : MessageListener() {

    private var messages: MutableList<Message> = mutableListOf()
    private var postmans: MutableMap<Int, StreamWorker> = mutableMapOf()
    private var games: MutableSet<Game> = mutableSetOf()
    private var awaitingPlayers: MutableSet<Int> = mutableSetOf()

    private var gameById: MutableMap<Int, Game> = mutableMapOf()

    private val postmansLock = Any()

    operator fun Pair<Combination, Any>.compareTo(other: Pair<Combination, Any>): Int {
        if (this.first < other.first) return 1
        if (this.first > other.first) return -1
        val kicker = this.second
        val otherKicker = other.second
        when (kicker) {
            is Suit -> return 0
            is Int -> return kicker.compareTo(otherKicker as Int)
            is Pair<*, *> -> {
                if (kicker.first is Int) {
                    val byFirst = (kicker.first as Int).compareTo((otherKicker as Pair<*, *>).first as Int)
                    if (byFirst != 0) return byFirst
                    return (kicker.second as Int).compareTo(otherKicker.second as Int)
                }
                return when {
                    this.first == Combination.FLUSH -> ((otherKicker as Pair<*, *>).second as Int).compareTo(kicker.second as Int)
                    this.first == Combination.STRAIGHT_FLUSH -> (kicker.second as Int).compareTo((otherKicker as Pair<*, *>).second as Int)
                    else -> 0
                }
            }
            else -> return 0
        }
    }

    override fun onMessage(message: Message) {
        println("${message.author}'th: " + message.text)
        messages.add(message)

        val currentGame = gameById.get(message.author)!!
        val currentPlayer = message.author
        var anotherPlayer = currentGame.second
        if (currentPlayer == anotherPlayer) anotherPlayer = currentGame.first

        if (message.command == "") postmans[anotherPlayer]?.sendMessage(message)
        else {
            if (((currentGame.first == currentPlayer) && (currentGame.currentState == STATE_FIRST_PLAYER_MOVING)) || (currentGame.second == currentPlayer && currentGame.currentState == STATE_SECOND_PLAYER_MOVING)) {
                try {
                    var roundEnded = false
                    when (message.command) {
                        "/m" -> if ((currentGame.highBet == null) || (currentGame.highBet!! < Combination.from(message.text))) {
                            currentGame.highBet = Combination.from(message.text)
                            postmans[anotherPlayer]?.sendMessage(message)
                            currentGame.changeTurn()
                            sendMovePrompts(currentGame)
                        } else postmans[currentPlayer]?.sendMessage(Message("> Incorrect move!", -1))
                        "/r" -> {
                            roundEnded = true
                            if (currentGame.highBet!!.first.exist(
                                    (currentGame.deckFirst).union(currentGame.deckSecond) as MutableSet<Card>,
                                    currentGame.highBet!!.second
                                )
                            ) currentGame.sendResults(postmans[anotherPlayer], postmans[currentPlayer], currentPlayer)
                            else currentGame.sendResults(postmans[currentPlayer], postmans[anotherPlayer], anotherPlayer)
                        }
                        "/b" -> {
                            roundEnded = true
                            if (currentGame.highBet!!.first.satisfy((currentGame.deckFirst).union(currentGame.deckSecond) as MutableSet<Card>) == currentGame.highBet!!.second) currentGame.sendResults(postmans[currentPlayer], postmans[anotherPlayer], anotherPlayer)
                            else currentGame.sendResults(postmans[anotherPlayer], postmans[currentPlayer], currentPlayer)
                        }
                    }
                    if ((roundEnded) && (currentGame.currentState != STATE_GAME_OVER)) startRound(currentGame.first, currentGame.second)
                }
                catch (e : IllegalArgumentException) {
                    postmans[currentPlayer]?.sendMessage(Message("> WRONG MOVE", -1))
                }
                catch (e: IndexOutOfBoundsException) {
                    postmans[currentPlayer]?.sendMessage(Message("> WRONG MOVE", -1))
                }
            } else postmans[currentPlayer]?.sendMessage(Message("> It's not your move!", -1))
        }
    }

    override fun onDisconnect(id: Int?) {
        println("$id: Some player disconnected from server")
    }

    fun Collection<Card>.print(): String {
        var s = ""
        for (element in this) {
            s += element.toString()
        }
        return s
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
                val secondPlayer = awaitingPlayers.first()
                val newGame = Game(counter, secondPlayer)
                awaitingPlayers.remove(secondPlayer)
                games.add(newGame)
                gameById.put(counter, newGame)
                gameById.put(secondPlayer, newGame)
                GlobalScope.launch {
                    newGame.init()
                    synchronized(postmansLock) {
                        when (newGame.currentState) {
                            STATE_SECOND_PLAYER_MOVING -> {
                                postmans[secondPlayer]?.sendMessage(
                                    Message(
                                        "> GAME WITH PLAYER #${newGame.first} STARTED: YOUR TURN",
                                        -1
                                    )
                                )
                                postman.sendMessage(Message("> GAME WITH PLAYER #$secondPlayer STARTED: WAIT", -1))
                                println("${newGame.first} vs. $secondPlayer")
                            }
                            STATE_FIRST_PLAYER_MOVING -> {
                                postman.sendMessage(Message("> GAME WITH PLAYER #$secondPlayer STARTED: YOUR TURN", -1))
                                postmans[secondPlayer]?.sendMessage(
                                    Message(
                                        "> GAME WITH PLAYER #${newGame.first} STARTED: WAIT",
                                        -1
                                    )
                                )
                                println("$secondPlayer vs. ${newGame.first}")
                            }
                            else -> {
                            }
                        }
                        startRound(newGame.first, newGame.second)
                    }

                }
            } else (awaitingPlayers.add(counter))
            synchronized(postmansLock) {
                postmans[counter++] = postman
//                for (message in messages) postman.sendMessage(message)
            }
        }
    }

    fun startRound(firstPlayer : Int, secondPlayer : Int) {
        val game = gameById[firstPlayer]!!
        postmans[secondPlayer]?.sendMessage(
            Message(
                game.deckSecond.sortedWith(
                    compareBy(
                        { it.face },
                        { it.suit })
                ).print()
            )
        )
        postmans[firstPlayer]?.sendMessage(
            Message(
                game.deckFirst.sortedWith(
                    compareBy(
                        { it.face },
                        { it.suit })
                ).print()
            )
        )
        sendMovePrompts(game)
    }

    fun sendMovePrompts(game : Game) {
        if (game.currentState == STATE_FIRST_PLAYER_MOVING) {
            postmans[game.first]?.sendMessage(Message("> MAKE YOUR MOVE"))
            postmans[game.second]?.sendMessage(Message("> WAIT FOR YOUR OPPONENT'S MOVE"))
        }
        else if (game.currentState == STATE_SECOND_PLAYER_MOVING) {
            postmans[game.first]?.sendMessage(Message("> WAIT FOR YOUR OPPONENT'S MOVE"))
            postmans[game.second]?.sendMessage(Message("> MAKE YOUR MOVE"))
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