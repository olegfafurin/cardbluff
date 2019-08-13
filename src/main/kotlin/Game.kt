package ru.drownshark.cardbluff

import java.security.SecureRandom
import kotlin.math.pow

const val STATE_BEFORE_BEGINNING = 0
const val STATE_FIRST_PLAYER_MOVING = 1
const val STATE_SECOND_PLAYER_MOVING = 2
const val STATE_GAME_OVER = 3

private const val K = 32 // K-factor

class Game(val playerOne: Player, val playerTwo: Player) {

    var currentState = 0

    var availableCards: MutableSet<Card> = mutableSetOf()

    var deckFirst: MutableSet<Card> = mutableSetOf()
    var deckSecond: MutableSet<Card> = mutableSetOf()

    private var firstBeginsRound = true

    var highBet: Pair<Combination, Any>? = null

    private fun fill() {
        availableCards.clear()
        for (value in Value.values()) {
            for (suit in Suit.values()) {
                availableCards.add(Card(value, suit))
            }
        }
    }

    fun init() {
        fill()
        deckFirst.clear()
        deckSecond.clear()
        highBet = null
        if (currentState == STATE_BEFORE_BEGINNING) {
            playerOne.nCards = 5
            playerTwo.nCards = 5
        }
        val sr = SecureRandom()
        for (i in 0 until playerOne.nCards + playerTwo.nCards) {
            val newCard = availableCards.elementAt(sr.nextInt(availableCards.size))
            availableCards.removeIf { it == newCard }
            if (i < playerOne.nCards) deckFirst.add(newCard)
            else deckSecond.add(newCard)
        }
        if (currentState == STATE_BEFORE_BEGINNING) {
            if (sr.nextBoolean()) {
                currentState = STATE_FIRST_PLAYER_MOVING
                firstBeginsRound = true
            } else {
                currentState = STATE_SECOND_PLAYER_MOVING
                firstBeginsRound = false
            }
        } else {
            firstBeginsRound = !firstBeginsRound
            currentState = if (firstBeginsRound) STATE_FIRST_PLAYER_MOVING
            else STATE_SECOND_PLAYER_MOVING
        }
    }

    fun changeTurn() {
        currentState = 3 - currentState
    }

    private fun revealCards(): String {
        var s = "ROUND RESULTS:\n"
        s += "${playerOne.id}'s cards:\n"
        for (card in deckFirst.sortedWith(
            compareBy(
                { it.face },
                { it.suit })
        )) s += card.toString()
        s += "${playerTwo.id}'s cards:\n"
        for (card in deckSecond.sortedWith(
            compareBy(
                { it.face },
                { it.suit })
        )) s += card.toString()
        return s
    }

    private fun addExtraCard(player: Player) {
        player.nCards++
    }

    fun sendResults(first: Player, second: Player) {
        val cards = revealCards()
        first.sendIncomingMessage(Message(cards))
        second.sendIncomingMessage(Message(cards))
        first.sendIncomingMessage(Message("> YOU WIN"))
        second.sendIncomingMessage(Message("> YOU LOSE"))
        addExtraCard(second)
    }

    fun finalize(winner: Player, loser: Player) {
        winner.sendIncomingMessage(Message("> YOU WIN THE GAME WITH @${loser.nickname}", -1, "<undefined>", "/exit"))
        loser.sendIncomingMessage(Message("> YOU LOSE THE GAME WITH @${winner.nickname}", -1, "<undefined>", "/exit"))
        updateRatings(winner, loser)
        currentState = STATE_GAME_OVER
    }

    private fun updateRatings(winner: Player, loser: Player) {
        try {
            val db = DBHelp("cardbluff")
            val r1 = db.getCurrentRating(winner.nickname)!!
            val r2 = db.getCurrentRating(loser.nickname)!!
            val R1 = 10.0.pow(r1/400)
            val R2 = 10.0.pow(r2/400)
            val E1 = R1 / (R1 + R2)
            val E2 = R2 / (R1 + R2)
            db.update(winner.nickname, r1 + K * (1 - E1))
            db.update(loser.nickname, r2 - K * E2)
            winner.sendIncomingMessage(Message("> YOUR RATING UPDATED TO ${r1 + K * (1 - E1)}", -1))
            loser.sendIncomingMessage(Message("> YOUR RATING UPDATED TO ${r2 - K * E2}", -1))
            return
        }
        catch (e: Exception) {
            println(e.message)
        }
    }
}