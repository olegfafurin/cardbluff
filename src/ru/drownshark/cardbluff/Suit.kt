package ru.drownshark.cardbluff

enum class Suit {
    HEARTS {
        override fun toString(): String {
            return "♥"
        }
    },
    DIAMONDS {
        override fun toString(): String {
            return "♦"
        }
    },
    SPADES {
        override fun toString(): String {
            return "♠"
        }
    },
    CLUBS {
        override fun toString(): String {
            return "♣"
        }
    }
}