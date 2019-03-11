package ru.drownshark.cardbluff

class Card(val face: Value, val suit: Suit) {
    override fun toString(): String {
        return "Suit: " + suit + "\nValue: " + face
    }

    override fun equals(other: Any?): Boolean {
        return if (other?.javaClass?.name == this.javaClass.name) {
            (this.face == (other as Card).face) && (this.suit == other.suit)
        } else false
    }

}