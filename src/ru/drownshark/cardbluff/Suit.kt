package ru.drownshark.cardbluff

enum class Suit {
    CLUBS {
        override fun toString(): String {
            return "♣"
        }
    },
    DIAMOND {
        override fun toString(): String {
            return "♦"
        }
    },
    SPADES {
        override fun toString(): String {
            return "♠"
        }
    },
    HEARTS {
        override fun toString(): String {
            return "♥"
        }
    }
}