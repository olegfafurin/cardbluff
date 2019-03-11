package ru.drownshark.cardbluff

import java.io.File

/**
 * Created by imd on 11/03/2019
 */

fun main(args: Array<String>) {
    var availableCards: MutableSet<Card> = mutableSetOf()

    fun fill() {
        availableCards.clear()
        for (value in Value.values()) {
            for (suit in Suit.values()){
                availableCards.add(Card(value, suit))
            }
        }
    }

    fun Collection<Card>.println() {
        for (card in this.sortedWith(compareBy({it.face}, {it.suit}))) println(card.face.name + " " + card.suit)
    }

    fun MutableSet<Card>.maxCombination() : Any {
        for (combination in Combination.values()) {
            val result = combination.satisfy(this)
            if (result != null) return Pair(combination.name, result)
        }
        return -1
    }

    for (testNum in 1..20) {
        fill()
        val testSet: MutableSet<Card> = mutableSetOf()
        for (i in 1..10) {
            val newCard = availableCards.toList().random()
            testSet.add(newCard)
            availableCards.removeIf { it == newCard }
        }
        File("test${testNum}").printWriter().use {
            it.println("____CARDS CHOSEN:_____")
            for (card in testSet.sortedWith(compareBy({it.face}, {it.suit}))) it.println(card.face.name + " " + card.suit)
            it.println(testSet.maxCombination())
        }
    }
}