package ru.drownshark.cardbluff

import java.lang.IllegalArgumentException
import kotlin.math.max
import kotlin.math.min

enum class Combination {
    ROYAL_FLUSH {
        override fun satisfy(set: MutableSet<Card>): Suit? {
            if (set.size < 5) return null
            for (suit in Suit.values()) {
                if (containsFiveHigh(set, suit)) return suit
            }
            return null
        }
    },
    STRAIGHT_FLUSH {
        override fun satisfy(set: MutableSet<Card>): Pair<Suit, Int>? {
            if (set.size < 5) return null
            var head: Card? = null
            for (suit in Suit.values()) {
                val suitCards = suitFilter(set, suit)
                val numbersByFace = suitCards.groupingBy { it.face }.eachCount().toList()
                    .sortedWith(compareByDescending<Pair<Value, Int>> { it.second }.thenByDescending { it.first })
                    .toMap()
                var counter = 0
                for (face in Value.values()) {
                    if (numbersByFace.containsKey(face)) {
                        counter++
                        if (((counter >= 5) || ((counter == 4) && (face == Value.FIVE) && (numbersByFace.containsKey(
                                Value.ACE
                            )))) && ((head == null) || (head.face.ordinal < face.ordinal))
                        ) head = Card(face, suit)
                    } else counter = 0
                }
            }
            return if (head == null) null else Pair(head.suit, head.face.number)
        }
    },
    FOUR {
        override fun satisfy(set: MutableSet<Card>): Int? {
            if (set.size < 4) return null
            val numbersByValue = set.groupingBy { it.face }.eachCount().toList()
                .sortedWith(compareByDescending<Pair<Value, Int>> { it.second }.thenByDescending { it.first })
            return if (numbersByValue.first().second == 4) numbersByValue.first().first.number else null
        }
    },
    FULL_HOUSE {
        override fun satisfy(set: MutableSet<Card>): Pair<Int, Int>? {
            if (set.size < 5) return null
            var list = set.groupingBy { it.face }.eachCount().toList()
            val manys = list.filter { it.second >= 3 }.sortedWith(compareByDescending { it.first })
            if (manys.isEmpty()) return null
            list =
                list.filter { ((it.first.number != manys.first().first.number) || (it.second != manys.first().second)) }
            val less = list.filter { it.second >= 2 }.sortedWith(compareByDescending { it.first })
            if (less.isEmpty()) return null
            return if (less[0].second >= 2) Pair(manys.first().first.number, less[0].first.number) else null
        }
    },
    FLUSH {
        override fun satisfy(set: MutableSet<Card>): Pair<Suit, Int>? {
            if (set.size < 5) return null
            val suitFilter: (Set<Card>, Suit) -> List<Card> = { mySet, mySuit -> mySet.filter { it.suit == mySuit } }
            var head: Card? = null
            for (suit in Suit.values()) {
                suitFilter(set, suit).sortedBy { it.face }.run {
                    if ((size >= 5) && ((head?.face ?: Value.ACE) >= this[4].face)) head = this[4]
                }
            }
            return if (head == null) null else Pair(head!!.suit, head!!.face.number)
        }
    },
    STRAIGHT {
        override fun satisfy(set: MutableSet<Card>): Int? {
            if (set.size < 5) return null
            val numbersByValue = set.groupingBy { it.face }.eachCount().toList()
                .sortedWith(compareByDescending<Pair<Value, Int>> { it.second }.thenByDescending { it.first }).toMap()
            var counter = 0
            var head: Value? = null
            for (face in Value.values()) {
                if (numbersByValue.containsKey(face)) {
                    counter++
                    if (((face == Value.FIVE) && (counter == 4) && (numbersByValue.containsKey(Value.ACE))) || (counter >= 5)) head =
                        face
                } else counter = 0
            }
            return head?.number
        }
    },
    THREE {
        override fun satisfy(set: MutableSet<Card>): Int? {
            val quantity = numbersByValue(set)
            return if (quantity.first().second >= 3) quantity.first().first.number else null
        }
    },
    TWO_PAIRS {
        override fun satisfy(set: MutableSet<Card>): Pair<Int, Int>? {
            val quantity = numbersByValue(set)
            return if ((quantity.size > 1) && (quantity.first().second >= 2) && (quantity[1].second >= 2)) Pair(
                quantity.first().first.number,
                quantity[1].first.number
            ) else null
        }
    },
    PAIR {
        override fun satisfy(set: MutableSet<Card>): Int? {
            val quantity = numbersByValue(set)
            return if (quantity.first().second >= 2) quantity.first().first.number else null
        }
    },
    HIGH_CARD {
        override fun satisfy(set: MutableSet<Card>): Int {
            return set.maxBy { it.face }!!.face.number
        }
    };

    abstract fun satisfy(set: MutableSet<Card>): Any?

    fun MutableSet<Card>.maxCombination(): Pair<Combination, Any>? {
        for (combination in Combination.values()) {
            val result = combination.satisfy(this)
            if (result != null) return Pair(combination, result)
        }
        return null
    }

    protected val containsFiveHigh: (Set<Card>, Suit) -> Boolean = { mySet, mySuit ->
        mySet.any { it == Card(Value.ACE, mySuit) } && mySet.any { it == Card(Value.KING, mySuit) } && mySet.any { it == Card(Value.QUEEN, mySuit) } && mySet.any { it == Card(Value.JACK, mySuit) } && mySet.any { it.equals(Card(Value.TEN, mySuit)) }
    }
    protected val suitFilter: (Set<Card>, Suit) -> List<Card> = { mySet, mySuit -> mySet.filter { it.suit == mySuit } }
    protected val numbersByValue: (MutableSet<Card>) -> List<Pair<Value, Int>> = { set ->
        set.groupingBy { it.face }.eachCount().toList()
            .sortedWith(compareByDescending<Pair<Value, Int>> { it.second }.thenByDescending { it.first })
    }

    fun exist(set: MutableSet<Card>, high: Any): Boolean {
        val h = satisfy(set) ?: return false
        if (h == high) return true
        when (this) {
            ROYAL_FLUSH -> {
                return containsFiveHigh(set, high as Suit)
            }
            STRAIGHT_FLUSH -> {
                val suit = (high as Pair<*, *>).first as Suit
                val face = Value.from(high.second as Int)
                val candidates = suitFilter(set, suit)
                for (i in 0..4) {
                    if (!candidates.any { it.face.number == face.number - i }) return false
                }
                return true
            }
            FOUR -> {
                val value = Value.from(high as Int)
                return numbersByValue(set).filter { it.second == 4 }.map { it.first }.contains(value)
            }
            FULL_HOUSE -> {
                val valueHigh = Value.from((high as Pair<*, *>).first as Int)
                val valueLow = Value.from(high.second as Int)
                return ((numbersByValue(set).toMap().getOrDefault(
                    valueHigh,
                    0
                ) >= 3) and (numbersByValue(set).toMap().getOrDefault(valueLow, 0) >= 2))
            }
            FLUSH -> {
                val suit = (high as Pair<*, *>).first as Suit
                val face = Value.from(high.second as Int)
                val suitable = suitFilter(set, suit).filter { it.face <= face }
                return ((suitable.size >= 5) && (suitable.any { it.face == face }))
            }
            STRAIGHT -> {
                val value = Value.from(high as Int)
                for (i in 0..4) {
                    if (!set.any { it.face.number == value.number - i }) return false
                }
                return true
            }
            THREE -> {
                val value = Value.from(high as Int)
                return numbersByValue(set).filter { it.second >= 3 }.map { it.first }.contains(value)
            }
            TWO_PAIRS -> {
                val valueHigh = Value.from((high as Pair<*, *>).first as Int)
                val valueLow = Value.from(high.second as Int)
                return numbersByValue(set).filter { it.second >= 2 }.map { it.first }
                    .containsAll(listOf(valueHigh, valueLow))
            }
            PAIR -> {
                val value = Value.from(high as Int)
                return numbersByValue(set).filter { it.second >= 2 }.map { it.first }.contains(value)
            }
            HIGH_CARD -> {
                return (set.any { it.face == Value.from(high as Int) })
            }
        }
    }

    companion object {
        fun from(s: String): Pair<Combination, Any> {
            val args = s.replace("\\s+".toRegex(), " ").split(" ")
            val c = Combination.valueOf(args[0])
            val a: Any
            a = when (c) {
                ROYAL_FLUSH -> {
                    if (args.size != 2) throw IllegalArgumentException()
                    else Suit.valueOf(args[1])
                }
                STRAIGHT_FLUSH, FLUSH -> {
                    if (args.size != 3) throw IllegalArgumentException()
                    else {
                        try {
                            val canonicalOrder = Pair(Suit.valueOf(args[1]), Value.valueOf(args[2]).number)
                            canonicalOrder
                        } catch (e: IllegalArgumentException) {
                            Pair(Suit.valueOf(args[2]), Value.valueOf(args[1]).number)
                        }
                    }
                }
                FOUR, THREE, PAIR, STRAIGHT, HIGH_CARD -> {
                    if (args.size != 2) throw IllegalArgumentException()
                    else Value.valueOf(args[1]).number
                }
                FULL_HOUSE -> {
                    if (args.size != 3) throw IllegalArgumentException()
                    else Pair(Value.valueOf(args[1]).number, Value.valueOf(args[2]).number)
                }
                TWO_PAIRS -> {
                    if (args.size != 3) throw IllegalArgumentException()
                    else Pair(
                        max(Value.valueOf(args[1]).number, Value.valueOf(args[2]).number),
                        min(Value.valueOf(args[1]).number, Value.valueOf(args[2]).number)
                    )
                }
            }
            if ((a is Pair<*, *> && a.first == a.second) || (c == STRAIGHT && (a as Int) < 5) || (c == FLUSH && ((a as Pair<*, *>).second as Int) < 7) || (c == STRAIGHT_FLUSH && ((a as Pair<*, *>).second as Int) < 5)) throw IllegalArgumentException()
            return Pair(c, a)
        }
    }
}