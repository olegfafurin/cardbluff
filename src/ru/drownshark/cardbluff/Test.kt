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

    val avgTimeWriter = File("avgTime").printWriter()

    for (cardsUnion in 1..52) {
        val startTime = System.currentTimeMillis()
        val testN = 100
        for (currentTest in 1..testN) {
            fill()
            val testSet: MutableSet<Card> = mutableSetOf()
            for (i in 1..cardsUnion) {
                val newCard = availableCards.toList().random()
                testSet.add(newCard)
                availableCards.removeIf { it == newCard }
            }
            File("cards${cardsUnion}_test${currentTest}.log").printWriter().use {
                it.println("____CARDS CHOSEN:____")
                for (card in testSet.sortedWith(compareBy({ it.face }, { it.suit }))) it.println(card.face.name + " " + card.suit)
                it.println("_MAXIMAL COMBINATION_")
                it.println(testSet.maxCombination())
            }
        }
        val stopTime = System.currentTimeMillis()
        val elapsedTime = stopTime - startTime
        println("Cards in deck: $cardsUnion, Execution time: $elapsedTime milliseconds")
        println("${elapsedTime.toDouble()*1000 / testN} μs in average")
        avgTimeWriter.println("$cardsUnion; ${elapsedTime.toDouble() / testN}")
    }
    avgTimeWriter.close()
}