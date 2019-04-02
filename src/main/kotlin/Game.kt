package ru.drownshark.cardbluff

import kotlinx.coroutines.internal.LockFreeLinkedListNode
import java.security.SecureRandom
import kotlin.math.max

const val STATE_BEFORE_BEGINNING = 0
const val STATE_FIRST_PLAYER_MOVING = 1
const val STATE_SECOND_PLAYER_MOVING = 2
const val STATE_GAME_OVER = 3

class Game(val first: Int, val second: Int) {

    var currentState = 0

    var availableCards: MutableSet<Card> = mutableSetOf()

    var deckFirst: MutableSet<Card> = mutableSetOf()
    var deckSecond: MutableSet<Card> = mutableSetOf()

    private var cardsFirst = 0
    private var cardsSecond = 0

    private var firstBeginsRound = true

    var highBet : Pair<Combination, Any>? = null

    fun fill() {
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
            cardsFirst = 5
            cardsSecond = 5
        }
        val sr = SecureRandom()
        for (i in 0 until cardsFirst + cardsSecond) {
            val newCard = availableCards.elementAt(sr.nextInt(availableCards.size))
            availableCards.removeIf { it == newCard }
            if (i < cardsFirst) deckFirst.add(newCard)
            else deckSecond.add(newCard)
        }
        if (currentState == STATE_BEFORE_BEGINNING) {
            if (sr.nextBoolean()) {
                currentState = STATE_FIRST_PLAYER_MOVING
                firstBeginsRound = true
            }
            else  {
                currentState = STATE_SECOND_PLAYER_MOVING
                firstBeginsRound = false
            }
        }
        else {
            firstBeginsRound = !firstBeginsRound
            currentState = if (firstBeginsRound) STATE_FIRST_PLAYER_MOVING
            else STATE_SECOND_PLAYER_MOVING
        }
    }

    fun changeTurn() {
        currentState = 3 - currentState
    }

    fun revealCards() : String {
        var s = "ROUND RESULTS:\n"
        s += "$first's cards:\n"
        for(card in deckFirst.sortedWith(
            compareBy(
                { it.face },
                { it.suit })
        )) s += card.toString()
        s += "$second's cards:\n"
        for(card in deckSecond.sortedWith(
            compareBy(
                { it.face },
                { it.suit })
        )) s += card.toString()
        return s
    }

    fun sendResults(first : StreamWorker?, second: StreamWorker?, indLose : Int) {
        val cards = revealCards()
        first?.sendMessage(Message(cards))
        second?.sendMessage(Message(cards))
        first?.sendMessage(Message("> YOU WIN"))
        second?.sendMessage(Message("> YOU LOSE"))
        if (indLose == this.first) cardsFirst++
        else cardsSecond++

        if ((cardsSecond >= cardsFirst + 2) && (cardsSecond >= 10)) finalize(this.first)
        else if ((cardsFirst >= cardsSecond + 2) && (cardsFirst>= 10)) finalize(this.second)
        else init()

    }

    fun finalize(indexWinner : Int) {

    }
}