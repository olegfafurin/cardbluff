package ru.drownshark.cardbluff

abstract class MessageListener {

    internal abstract fun onMessage(message: Message)

    internal abstract fun onDisconnect(id: Int? = null)

    internal open fun onException(e: Exception) {
        e.printStackTrace()
    }
}
