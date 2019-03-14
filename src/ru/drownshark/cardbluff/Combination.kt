package ru.drownshark.cardbluff

enum class Combination {
    ROYAL_FLUSH {
        override fun satisfy(set: MutableSet<Card>): Suit? {
            if (set.size < 5) return null
            val containsFiveHigh: (Set<Card>, Suit) -> Boolean = { mySet, mySuit -> mySet.any { it.equals(Card(Value.ACE, mySuit)) } && mySet.any { it.equals(Card(Value.KING, mySuit)) } && mySet.any { it.equals(Card(Value.QUEEN, mySuit)) } && mySet.any { it.equals(Card(Value.JACK, mySuit)) } && mySet.any { it.equals(Card(Value.TEN, mySuit)) } }
            for (suit in Suit.values()) {
                if (containsFiveHigh(set, suit)) return suit
            }
            return null
        }
    },
    STRAIGHT_FLUSH {
        override fun satisfy(set: MutableSet<Card>): Pair<Suit, Int>? {
            if (set.size < 5) return null
            val suitFilter: (Set<Card>, Suit) -> List<Card> = { mySet, mySuit -> mySet.filter { it.suit == mySuit } }
            var head: Card? = null
            for (suit in Suit.values()) {
                val suitCards = suitFilter(set, suit)
                val numbersByFace = suitCards.groupingBy { it.face }.eachCount().toList().sortedWith(compareByDescending<Pair<Value, Int>> { it.second }.thenByDescending { it.first }).toMap()
                var counter = 0
                for (face in Value.values()) {
                    if (numbersByFace.containsKey(face)) {
                        counter++
                        if (((counter >= 5) || ((counter == 4) && (face == Value.FIVE) && (numbersByFace.containsKey(Value.ACE)))) && ((head == null) || (head.face.ordinal < face.ordinal))) head = Card(face, suit)
                    } else counter = 0
                }
            }
            return if (head == null) null else Pair(head.suit, head.face.number)
        }
    },
    FOUR {
        override fun satisfy(set: MutableSet<Card>): Int? {
            if (set.size < 4) return null
            val numbersByValue = set.groupingBy { it.face }.eachCount().toList().sortedWith(compareByDescending<Pair<Value, Int>> { it.second }.thenByDescending { it.first })
            return if (numbersByValue.first().second == 4) numbersByValue.first().first.number else null
        }
    },
    FULL_HOUSE {
        override fun satisfy(set: MutableSet<Card>): Pair<Int, Int>? {
            if (set.size < 5) return null
            var list = set.groupingBy { it.face }.eachCount().toList()
            val manys = list.filter { it.second >= 3 }.sortedWith(compareByDescending<Pair<Value, Int>> { it.first })
            if (manys.size == 0) return null
            list = list.filter { ((it.first.number != manys.first().first.number) || (it.second != manys.first().second)) }
            val less = list.filter { it.second >= 2 }.sortedWith(compareByDescending<Pair<Value, Int>> { it.first })
            if (less.size == 0) return null
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
            val numbersByValue = set.groupingBy { it.face }.eachCount().toList().sortedWith(compareByDescending<Pair<Value, Int>> { it.second }.thenByDescending { it.first }).toMap()
            var counter = 0
            var head: Value? = null
            for (face in Value.values()) {
                if (numbersByValue.containsKey(face)) {
                    counter++
                    if (((face == Value.FIVE) && (counter == 4) && (numbersByValue.containsKey(Value.ACE))) || (counter >= 5)) head = face
                } else counter = 0
            }
            return head?.number
        }
    },
    THREE {
        override fun satisfy(set: MutableSet<Card>): Int? {
            val numbersByValue = set.groupingBy { it.face }.eachCount().toList().sortedWith(compareByDescending<Pair<Value, Int>> { it.second }.thenByDescending { it.first })
            return if (numbersByValue.first().second == 3) numbersByValue.first().first.number else null
        }
    },
    TWO_PAIRS {
        override fun satisfy(set: MutableSet<Card>): Pair<Int, Int>? {
            val numbersByValue = set.groupingBy { it.face }.eachCount().toList().sortedWith(compareByDescending<Pair<Value, Int>> { it.second }.thenByDescending { it.first })
            return if ((numbersByValue.size > 1) && (numbersByValue.first().second == 2) && (numbersByValue[1].second == 2)) Pair(numbersByValue.first().first.number, numbersByValue[1].first.number) else null
        }
    },
    PAIR {
        override fun satisfy(set: MutableSet<Card>): Int? {
            val numbersByValue = set.groupingBy { it.face }.eachCount().toList().sortedWith(compareByDescending<Pair<Value, Int>> { it.second }.thenByDescending { it.first })
            return if (numbersByValue.first().second == 2) numbersByValue.first().first.number else null
        }
    },
    HIGH_CARD {
        override fun satisfy(set: MutableSet<Card>): Int {
            return set.maxBy { it.face }!!.face.number
        }
    };

    abstract fun satisfy(set: MutableSet<Card>): Any?

}