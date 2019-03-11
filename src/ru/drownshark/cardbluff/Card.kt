package ru.drownshark.cardbluff

class Card(val face: Value, val suit : Suit) {
    override fun toString() : String {
        return ("Suit: " + suit + "\nValue: " + face)
    }

    fun equals(other: Card): Boolean {
        return (this.face == other.face) && (this.suit == other.suit)
    }
}