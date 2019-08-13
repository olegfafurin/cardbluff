package ru.drownshark.cardbluff

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.security.MessageDigest

class CardBluffServer(var connect: ServerSocket) : MessageListener(), Runnable {

    private var messages: MutableList<Message> = mutableListOf()
    private var players: MutableMap<Int, Player> = mutableMapOf()
    private var games: MutableSet<Game> = mutableSetOf()
    private var awaitingPlayers: MutableSet<Player> = mutableSetOf()
    private var authorised: MutableSet<String> = mutableSetOf()

    private var gameById: MutableMap<Int, Game> = mutableMapOf()

    private val postmansLock = Any()

    fun MutableSet<Card>.maxCombination(): Pair<Combination, Any>? {
        for (combination in Combination.values()) {
            val result = combination.satisfy(this)
            if (result != null) return Pair(combination, result)
        }
        return null
    }

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
        println("@${message.authorNickname}: " + message.text)
        messages.add(message)
        val game = gameById[message.author]
        if (game == null && message.command != "/exit") {
            players[message.author]!!.sendIncomingMessage(Message("> GAME HAS NOT STARTED YET", -1, "SERVER"))
            return
        } else if (game == null) {
            players[message.author]!!.sendIncomingMessage(Message("> GAME HAS NOT STARTED YET", -1, "SERVER"))
            return
        }
        val currentPlayer = players[message.author]
        var anotherPlayer = game.playerTwo
        if (currentPlayer == anotherPlayer) anotherPlayer = game.playerOne

        if (message.command == "/exit") {
            gameById.remove(currentPlayer!!.id)
            gameById.remove(anotherPlayer.id)
//            players.remove(currentPlayer.id)
//            players.remove(anotherPlayer.id)
            currentPlayer.sendIncomingMessage(Message("> GAME INTERRUPTED", -1, "SERVER"))
            anotherPlayer.sendIncomingMessage(Message("> GAME INTERRUPTED", -1, "SERVER"))
            anotherPlayer.stopPostman()
//            currentPlayer.stopPostman()
//            anotherPlayer.stopPostman()
            println("Interrupted: ${currentPlayer.nickname} (${currentPlayer.id}) vs. ${anotherPlayer.nickname} (${anotherPlayer.id})")
            return
        }

        if (message.command == "") anotherPlayer.sendIncomingMessage(message)
        else {
            if (((game.playerOne == currentPlayer) && (game.currentState == STATE_FIRST_PLAYER_MOVING)) || (game.playerTwo == currentPlayer && game.currentState == STATE_SECOND_PLAYER_MOVING)) {
                try {
                    var roundEnded = false
                    when (message.command) {
                        "/m" -> if ((game.highBet == null) || (game.highBet!! < Combination.from(message.text))) {
                            game.highBet = Combination.from(message.text)
                            anotherPlayer.sendIncomingMessage(
                                Message(
                                    Combination.from(message.text).run { "$first ${second.comDetails()}" },
                                    message.author,
                                    message.authorNickname
                                )
                            )
                            game.changeTurn()
                            sendMovePrompts(game)
                        } else currentPlayer.sendIncomingMessage(Message("> INCORRECT MOVE!", -1))
                        "/r" -> {
                            if (game.highBet == null) {
                                currentPlayer.sendIncomingMessage(Message("> CAN'T REVEAL WITHOUT ANY BET!", -1))
                                return
                            }
                            roundEnded = true
                            if (game.highBet!!.first.exist(
                                    (game.deckFirst).union(game.deckSecond) as MutableSet<Card>,
                                    game.highBet!!.second
                                )
                            ) endRound(game, anotherPlayer, currentPlayer)
                            else endRound(game, currentPlayer, anotherPlayer)
                        }
                        "/b" -> {
                            if (game.highBet == null) {
                                currentPlayer.sendIncomingMessage(Message("> CAN'T BLOCK WITHOUT ANY BET!", -1))
                                return
                            }
                            roundEnded = true
                            if (game.highBet!!.first.exist((game.deckFirst).union(game.deckSecond) as MutableSet<Card>, game.highBet!!.second) && ((game.deckFirst).union(
                                    game.deckSecond
                                ) as MutableSet<Card>).maxCombination()!!.first == game.highBet!!.first
                            ) // if current bet exists and is a maxximal combination
                                endRound(
                                    game,
                                    currentPlayer,
                                    anotherPlayer
                                )
                            else endRound(game, anotherPlayer, currentPlayer)
                        }
                    }
                    if ((roundEnded) && (game.currentState != STATE_GAME_OVER)) startRound(
                        game.playerOne.id,
                        game.playerTwo.id
                    )
                } catch (e: IllegalArgumentException) {
                    currentPlayer.sendIncomingMessage(Message("> WRONG MOVE, IAE", -1))
                } catch (e: IndexOutOfBoundsException) {
                    currentPlayer.sendIncomingMessage(Message("> WRONG MOVE", -1))
                } catch (e: SocketException) {
                    currentPlayer.sendIncomingMessage(Message("> YOUR PARTNER HAS DISCONNECTED", -1))
                    game.finalize(currentPlayer, anotherPlayer)
                    gameById.remove(currentPlayer.id)
                    gameById.remove(anotherPlayer.id)
                    players.remove(currentPlayer.id)
                    players.remove(anotherPlayer.id)
                    awaitingPlayers.add(currentPlayer)
                }
            } else currentPlayer?.sendIncomingMessage(Message("> IT'S NOT YOUR MOVE!", -1))
        }
    }

    // TODO: Add time limit on move

    private fun endRound(game: Game, playerOne: Player, playerTwo: Player) {
        game.sendResults(playerOne, playerTwo)
        if ((playerTwo.nCards >= playerOne.nCards + 1) && (playerTwo.nCards >= 5)) {
            game.finalize(playerOne, playerTwo)
            gameById.remove(playerOne.id)
            gameById.remove(playerTwo.id)
            players.remove(playerOne.id)
            players.remove(playerTwo.id)
        } else if ((playerOne.nCards >= playerTwo.nCards + 1) && (playerOne.nCards >= 5)) {
            game.finalize(playerTwo, playerOne)
            gameById.remove(playerOne.id)
            gameById.remove(playerTwo.id)
            players.remove(playerOne.id)
            players.remove(playerTwo.id)
        } else game.init()
    }

    override fun onDisconnect(id: Int?) {
        println("$id: Some player disconnected from server")
        if (awaitingPlayers.isNotEmpty() && awaitingPlayers.first().id == id) awaitingPlayers.remove(players[id])
        else {
            val endedGame = gameById[id]
            val otherPlayerId =
                if (endedGame?.playerOne?.id == id) endedGame?.playerTwo?.id else endedGame?.playerOne?.id
            gameById.remove(otherPlayerId)
            gameById.remove(id)
            players[otherPlayerId]?.sendIncomingMessage(
                Message(
                    "> YOUR PARTNER HAS DISCONNECTED",
                    -1,
                    "<undefined>",
                    "/exit"
                )
            )
            players.remove(id)
            players.remove(otherPlayerId)
//            awaitingPlayers.add(players[otherPlayerId]!!)
        }
    }

    fun Collection<Card>.print(): String {
        var s = ""
        for (element in this) {
            s += element.toString()
        }
        return s
    }

    fun start() {
        val thread = Thread(this, "ClientHandler")
        thread.start()
    }

    override fun run() {
        val client = connect.accept()
        start()
        val counter: Int
        synchronized(globalCounter) {
            counter = globalCounter++
        }
        println(">> Player #$counter connected")

        val authMsg = ObjectInputStream(client.getInputStream()).readObject() as Message // recieve a username
        if (authMsg.text !in authorised && !auth(client, authMsg.text)) {
            ObjectOutputStream(client.getOutputStream()).run {
                writeObject(
                    Message(
                        "Wrong password",
                        counter,
                        "<undefined>"
                    )
                )
            } // send the ID just for fun
            return
        }
        authorised.add(authMsg.text)
        ObjectOutputStream(client.getOutputStream()).run {
            writeObject(
                Message(
                    "",
                    counter,
                    "<undefined>"
                )
            )
        } // send the ID as follows
        val postman = StreamWorker(client.getInputStream(), client.getOutputStream(), counter)
        postman.addListener(this@CardBluffServer)
        postman.start()

        val newPlayer = Player(counter, postman, authMsg.text)
        if (awaitingPlayers.isNotEmpty()) {
            val otherPlayer = awaitingPlayers.first()
            otherPlayer.sendIncomingMessage(Message("> READY", -1))
            val newGame = Game(newPlayer, otherPlayer)
            try {
                awaitingPlayers.remove(otherPlayer)
                games.add(newGame)
                gameById[counter] = newGame
                gameById[otherPlayer.id] = newGame
                GlobalScope.launch {
                    newGame.init()
                    synchronized(postmansLock) {
                        when (newGame.currentState) {
                            STATE_SECOND_PLAYER_MOVING -> {
                                otherPlayer.sendIncomingMessage(
                                    Message(
                                        "> GAME WITH PLAYER @${newGame.playerOne.nickname} STARTED: YOUR TURN",
                                        -1
                                    )
                                )
                                postman.sendMessage(
                                    Message(
                                        "> GAME WITH PLAYER @${newGame.playerTwo.nickname} STARTED: WAIT",
                                        -1
                                    )
                                )
                                println("${newGame.playerOne.nickname} (${newGame.playerOne.id}) vs. ${newGame.playerTwo.nickname} (${newGame.playerTwo.id})")
                            }
                            STATE_FIRST_PLAYER_MOVING -> {
                                postman.sendMessage(
                                    Message(
                                        "> GAME WITH PLAYER @${newGame.playerTwo.nickname} STARTED: YOUR TURN",
                                        -1
                                    )
                                )
                                otherPlayer.sendIncomingMessage(
                                    Message(
                                        "> GAME WITH PLAYER @${newGame.playerOne.nickname} STARTED: WAIT",
                                        -1
                                    )
                                )
                                println("${newGame.playerTwo.nickname} (${newGame.playerTwo.id}) vs. ${newGame.playerOne.nickname} (${newGame.playerOne.id})")
                            }
                            else -> {
                            }
                        }
                        startRound(newGame.playerOne.id, newGame.playerTwo.id)
                    }

                }
            } catch (e: SocketException) {
                if (e.message == "Socket closed") {
                    awaitingPlayers.remove(otherPlayer)
                    awaitingPlayers.add(newPlayer)
                }
            } catch (e: InterruptedException) {
                println("Interrupted: ${newGame.playerOne.nickname} (${newGame.playerOne.id}) vs. ${newGame.playerTwo.nickname} (${newGame.playerTwo.id})")
            }
        } else (awaitingPlayers.add(newPlayer))
        synchronized(postmansLock) {
            players[counter] = newPlayer
//                for (message in messages) postman.sendIncomingMessage(message)
        }
    }

    private fun getHash(s: String): String {
        return bytesToHex(MessageDigest.getInstance("SHA-256").digest(s.toByteArray()))
    }

    private fun bytesToHex(hash: ByteArray): String {
        val hexString = StringBuffer()
        for (i in 0 until hash.size) {
            val hex = Integer.toHexString(0xff and hash[i].toInt())
            if (hex.length == 1) hexString.append('0')
            hexString.append(hex)
        }
        return hexString.toString()
    }

    private fun auth(client: Socket, login: String): Boolean {
        val db = DBHelp("cardbluff")
        val passwdHash = db.getPasswordHash(login)
        if (passwdHash != null) {
            ObjectOutputStream(client.getOutputStream()).run {
                writeObject(
                    Message(
                        "Enter your password",
                        -1,
                        "<undefined>",
                        "/auth"
                    )
                )
            } // request the password
            val answer = ObjectInputStream(client.getInputStream()).readObject() as Message // and receive it
            return getHash(answer.text) == passwdHash
        } else {
            ObjectOutputStream(client.getOutputStream()).run {
                writeObject(
                    Message(
                        "No user found: create your password",
                        -1,
                        "<undefined>",
                        "/auth"
                    )
                )
            } // or request the password creation
            db.insert(
                login,
                getHash((ObjectInputStream(client.getInputStream()).readObject() as Message).text),
                100.0
            ) // and receive it
            return true
        }
    }

    fun startRound(firstPlayer: Int, secondPlayer: Int) {
        val game = gameById[firstPlayer]!!
        players[secondPlayer]?.sendIncomingMessage(
            Message(
                game.deckSecond.sortedWith(
                    compareBy(
                        { it.face },
                        { it.suit })
                ).print()
            )
        )
        players[firstPlayer]?.sendIncomingMessage(
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

    fun sendMovePrompts(game: Game) {
        if (game.currentState == STATE_FIRST_PLAYER_MOVING) {
            game.playerOne.sendIncomingMessage(Message("> MAKE YOUR MOVE", -1))
            game.playerTwo.sendIncomingMessage(Message("> WAIT FOR YOUR OPPONENT'S MOVE", -1))
        } else if (game.currentState == STATE_SECOND_PLAYER_MOVING) {
            game.playerOne.sendIncomingMessage(Message("> WAIT FOR YOUR OPPONENT'S MOVE", -1))
            game.playerTwo.sendIncomingMessage(Message("> MAKE YOUR MOVE", -1))
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            CardBluffServer(ServerSocket(1239)).run { start() }
        }

        @JvmStatic
        var globalCounter: Int = 0
    }

    override fun onException(e: Exception) {
//        println(e.message)
        println("Something had gone wrong...")
//        super.onException(e)
    }
}