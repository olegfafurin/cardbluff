package ru.drownshark.cardbluff

import kotlin.math.max

enum class Combination(ind: Int) {
    ROYAL_FLUSH(0) {
        override fun satisfy(set: MutableSet<Card>): Int? {
//            set.
            val containsFiveHigh : (Set<Card>, Suit) -> Boolean = {mySet, mySuit -> mySet.any{it.equals(Card(Value.ACE, mySuit))} && mySet.any{it.equals(Card(Value.KING, mySuit))} && mySet.any{it.equals(Card(Value.QUEEN, mySuit))} && mySet.any{it.equals(Card(Value.JACK, mySuit))} && mySet.any{it.equals(Card(Value.TEN, mySuit))} }
            for (suit in Suit.values()) {
                if (containsFiveHigh(set, suit)) return suit.ordinal
            }
            return null
        }
    },
    STRAIGHT_FLUSH(1) {
        override fun satisfy(set: MutableSet<Card>): Int? {
            val suitFilter: (Set<Card>, Suit) -> List<Card> = { mySet, mySuit -> mySet.filter { it.suit == mySuit } }
            var head: Value? = null
            for (suit in Suit.values()) {
                val numbersByFace = suitFilter(set, suit).groupingBy { it.face }.eachCount().toSortedMap(reverseOrder())
                var counter = 0
                for (face in Value.values()) {
                    if (numbersByFace.containsKey(face)) {
                        counter++
                        if ((counter >= 5) && ((head == null) || (head.ordinal < face.ordinal))) head = face
                    } else counter = 0
                }
            }
            return head?.ordinal
        }
    },
    FOUR(2) {
        override fun satisfy(set: MutableSet<Card>): Int? {
            val numbersByElement = set.groupingBy { it.face }.eachCount().toSortedMap(reverseOrder())
            val values = numbersByElement.keys.asIterable()
            return if (numbersByElement[values.first()]!! == 4) values.first().ordinal else null
        }
    },
    FULL_HOUSE(3) {
        override fun satisfy(set: MutableSet<Card>): Pair<Int, Int>? {
            val numbersByElement = set.groupingBy { it.face }.eachCount().toSortedMap(reverseOrder())
            val values = numbersByElement.keys.asIterable()
            if (values.elementAtOrNull(1) == null) return null
            return if ((numbersByElement[values.first()]!! >= 3) && (numbersByElement[values.elementAt(1)]!! >= 2)) Pair(values.first().ordinal, values.elementAt(1).ordinal) else null
        }
    },
    FLUSH(4) {
        override fun satisfy(set: MutableSet<Card>): Int? {
            if (set.size < 5) return null
            val numbersByElement = set.groupingBy { it.suit }.eachCount().toSortedMap(reverseOrder())
            val values = numbersByElement.keys.asIterable()
            return if (numbersByElement[values.first()]!! >= 5) values.first().ordinal else null
        }
    },
    STRAIGHT(5) {
        override fun satisfy(set: MutableSet<Card>): Int? {
            if (set.size < 5) return null
            val numbersByElement = set.groupingBy { it.face }.eachCount().toSortedMap(reverseOrder())
            var counter = 0
            var head: Value? = null
            for (face in Value.values()) {
                if (numbersByElement.containsKey(face)) {
                    counter++
                    if ((counter == 4) && (numbersByElement.containsKey(Value.ACE))) return (Value.values().map { it.ordinal }).max()!! + 1
                    if (counter >= 5) head = face
                } else counter = 0
            }
            return head?.ordinal
        }
    },
    THREE(6) {
        override fun satisfy(set: MutableSet<Card>): Int? {
            val numbersByElement = set.groupingBy { it.face }.eachCount().toSortedMap(reverseOrder())
            val values = numbersByElement.keys.asIterable()
            return if (numbersByElement[values.first()]!! >= 3) values.first().ordinal else null
        }
    },
    TWO_PAIRS(7) {
        override fun satisfy(set: MutableSet<Card>): Pair<Int, Int>? {
            val numbersByElement = set.groupingBy { it.face }.eachCount().toSortedMap(reverseOrder())
            val values = numbersByElement.keys.asIterable()
            if (values.elementAtOrNull(1) == null) return null
            return if ((numbersByElement[values.first()]!! >= 2) && (numbersByElement[values.elementAtOrElse(1) {0}]!! >= 2)) Pair(values.first().ordinal, values.elementAt(1).ordinal) else null
        }
    },
    PAIR(8) {
        override fun satisfy(set: MutableSet<Card>): Int? {
            val numbersByElement = set.groupingBy { it.face }.eachCount().toSortedMap(reverseOrder())
            val values = numbersByElement.keys.asIterable()
            return if (numbersByElement[values.first()]!! >= 2) values.first().ordinal else null
        }
    },
    HIGH_CARD(9) {
        override fun satisfy(set: MutableSet<Card>): Int {
            return set.maxBy { it.face }!!.face.ordinal
        }
    };

    abstract fun satisfy(set: MutableSet<Card>): Any?

}