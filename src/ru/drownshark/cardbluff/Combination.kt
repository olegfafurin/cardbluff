package ru.drownshark.cardbluff

enum class Combination() {
    ROYAL_FLUSH {
        override fun satisfy(set: MutableSet<Card>): Int? {
            val containsFiveHigh : (Set<Card>, Suit) -> Boolean = {mySet, mySuit -> mySet.any{it.equals(Card(Value.ACE, mySuit))} && mySet.any{it.equals(Card(Value.KING, mySuit))} && mySet.any{it.equals(Card(Value.QUEEN, mySuit))} && mySet.any{it.equals(Card(Value.JACK, mySuit))} && mySet.any{it.equals(Card(Value.TEN, mySuit))} }
            for (suit in Suit.values()) {
                if (containsFiveHigh(set, suit)) return suit.ordinal
            }
            return null
        }
    },
    STRAIGHT_FLUSH {
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
            return head?.number
        }
    },
    FOUR {
        override fun satisfy(set: MutableSet<Card>): Int? {
            val numbersByValue = set.groupingBy { it.face }.eachCount().toList().sortedWith(compareByDescending <Pair<Value,Int>>{it.second}.thenByDescending{it.first})
            return if (numbersByValue.first().second == 4) numbersByValue.first().first.number else null
        }
    },
    FULL_HOUSE {
        override fun satisfy(set: MutableSet<Card>): Pair<Int, Int>? {
            val numbersByValue = set.groupingBy { it.face }.eachCount().toList().sortedWith(compareByDescending <Pair<Value,Int>>{it.second}.thenByDescending{it.first})
            return if ((numbersByValue.first().second == 3) && (numbersByValue[1].second >= 2)) Pair(numbersByValue.first().first.number, numbersByValue[1].first.number) else null
        }
    },
    FLUSH {
        override fun satisfy(set: MutableSet<Card>): Int? {
            if (set.size < 5) return null
            val numbersBySuit = set.groupingBy { it.suit }.eachCount().toSortedMap()
            val values = numbersBySuit.keys.asIterable()
            return if (numbersBySuit.getValue(values.first()) >= 5) values.first().ordinal else null
        }
    },
    STRAIGHT {
        override fun satisfy(set: MutableSet<Card>): Int? {
            if (set.size < 5) return null
            val numbersByValue = set.groupingBy { it.face }.eachCount().toList().sortedWith(compareByDescending <Pair<Value,Int>>{it.second}.thenByDescending{it.first}).toMap()
            var counter = 0
            var head: Value? = null
            for (face in Value.values()) {
                if (numbersByValue.containsKey(face)) {
                    counter++
                    if ((face == Value.FIVE) && (counter == 4) && (numbersByValue.containsKey(Value.ACE))) return (Value.values().map { it.number }).max()!! + 1
                    if (counter >= 5) head = face
                } else counter = 0
            }
            return head?.number
        }
    },
    THREE {
        override fun satisfy(set: MutableSet<Card>): Int? {
            val numbersByValue = set.groupingBy { it.face }.eachCount().toList().sortedWith(compareByDescending <Pair<Value,Int>>{it.second}.thenByDescending{it.first})
            return if (numbersByValue.first().second == 3) numbersByValue.first().first.number else null
        }
    },
    TWO_PAIRS {
        override fun satisfy(set: MutableSet<Card>): Pair<Int, Int>? {
            val numbersByValue = set.groupingBy { it.face }.eachCount().toList().sortedWith(compareByDescending <Pair<Value,Int>>{it.second}.thenByDescending{it.first})
            return if ((numbersByValue.first().second == 2) && (numbersByValue[1].second == 2)) Pair(numbersByValue.first().first.number, numbersByValue[1].first.number) else null
        }
    },
    PAIR {
        override fun satisfy(set: MutableSet<Card>): Int? {
            val numbersByValue = set.groupingBy { it.face }.eachCount().toList().sortedWith(compareByDescending <Pair<Value,Int>>{it.second}.thenByDescending{it.first})
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