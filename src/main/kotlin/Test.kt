package ru.drownshark.cardbluff

import java.io.File

/**
 * Created by imd on 11/03/2019
 */
fun Any.comDetails(): String {
    if (this is Pair<*, *>) {
        if ((this.first is Int) && (this.second is Int)) {
            return faceNameByValue(this.first as Int) + " " + faceNameByValue(this.second as Int)
        } else if ((this.first is Suit) && (this.second is Int)) {
            return (this.first as Suit).name + " " + faceNameByValue(this.second as Int)
        }
    } else if (this is Int) {
        return faceNameByValue(this)!!
    } else if (this is Suit) {
        return this.name
    }
    return "<ERROR>"
}



fun faceNameByValue(num: Int): String? {
    return Value.values().find { it.number == num }?.name
}

fun suitNameByValue(ord: Int): String? {
    return Suit.values().find { it.ordinal == ord }?.name
}

fun main(args: Array<String>) {
    var availableCards: MutableSet<Card> = mutableSetOf()

    fun fill() {
        availableCards.clear()
        for (value in Value.values()) {
            for (suit in Suit.values()) {
                availableCards.add(Card(value, suit))
            }
        }
    }

    fun Collection<Card>.println() {
        for (card in this.sortedWith(compareBy({ it.face }, { it.suit }))) println(card.face.name + " " + card.suit)
    }


//    val avgTimeWriter = File("avgTime").printWriter()
//    for (cardsUnion in 1..52) {
//        val startTime = System.currentTimeMillis()
//        val testN = 1000
//        for (currentTest in 1..testN) {
//            fill()
//            val testSet: MutableSet<Card> = mutableSetOf()
//            for (i in 1..cardsUnion) {
//                val newCard = availableCards.toList().random()
//                testSet.add(newCard)
//                availableCards.removeIf { it == newCard }
//            }
//            val combo = testSet.maxCombination()
//            File("cards${cardsUnion}_test${currentTest}.log").printWriter().use {
//                it.println("____CARDS CHOSEN:____")
//                for (card in testSet.sortedWith(
//                    compareBy(
//                        { it.face },
//                        { it.suit })
//                )) it.println(card.face.name + " " + card.suit)
//                it.println("_MAXIMAL COMBINATION_")
//                it.print("${combo!!.first}: ${combo.second.comDetails()}")
//            }
//            File("cards${cardsUnion}_test${currentTest}_formatted.log").printWriter().use {
//                it.println(cardsUnion)
//                for (card in testSet.sortedWith(
//                    compareBy(
//                        { it.face },
//                        { it.suit })
//                )) it.print("${card.face.ordinal * 4 + card.suit.ordinal} ")
//                it.println()
//                it.print("${combo!!.first}: ${combo.second.comDetails()}")
//            }
//
//        }
//        val stopTime = System.currentTimeMillis()
//        val elapsedTime = stopTime - startTime
//        println("Cards in deck: $cardsUnion, Execution time: $elapsedTime milliseconds")
//        println("${elapsedTime.toDouble() / testN} ms in average")
//        avgTimeWriter.println("$cardsUnion; ${elapsedTime.toDouble() / testN}")
//    }

    val testSet = mutableSetOf(
        Card(Value.THREE, Suit.HEARTS),
        Card(Value.THREE, Suit.DIAMONDS),
        Card(Value.TEN, Suit.SPADES),
        Card(Value.JACK, Suit.CLUBS),
        Card(Value.KING, Suit.DIAMONDS),
        Card(Value.TWO, Suit.SPADES),
        Card(Value.THREE, Suit.CLUBS),
        Card(Value.SIX, Suit.DIAMONDS),
        Card(Value.SEVEN, Suit.SPADES),
        Card(Value.EIGHT, Suit.DIAMONDS),
        Card(Value.KING, Suit.HEARTS)
        )


//    println(testSet.maxCombination())
//    println(Combination.FLUSH.exist(testSet, Pair(Suit.HEARTS, 13)))



    println(Combination.TWO_PAIRS.exist(testSet, Pair(13,3)))

//    avgTimeWriter.close()
}