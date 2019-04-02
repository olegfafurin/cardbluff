package ru.drownshark.cardbluff

import java.io.*
import java.net.SocketException
import java.util.ArrayList
import kotlin.concurrent.thread

class StreamWorker(var input: InputStream, var output: OutputStream) : Runnable, Closeable {

    private val listeners = ArrayList<MessageListener>()

    private val outputLock = Any()
    private val listenerLock = Any()

    fun addListener(listener: MessageListener) {
        listeners.add(listener)
    }

    override fun run() {
        try {
            var m: Message
            while (true) {
                val inputData = ObjectInputStream(input)
                m = inputData.readObject() as Message
                synchronized(listenerLock) {
                    for (listener in listeners) {
                        listener.onMessage(m)
                    }
                }
            }
        } catch (e: SocketException) {
            if (e.message == "Connection reset") {
                synchronized(listenerLock) {
                    for (listener in listeners) {
                        listener.onDisconnect()
                    }
                }
            } else {
                synchronized(listenerLock) {
                    for (listener in listeners) {
                        listener.onException(e)
                    }
                }
            }
        } catch (e: IOException) {
            synchronized(listenerLock) {
                for (listener in listeners) {
                    listener.onException(e)
                }
            }
        }
//        close()
    }

    fun start() {
        val thread = Thread(this, "StreamWorker")
        thread.start()
    }

    fun sendMessage(message: Message) {
        synchronized(outputLock) {
            ObjectOutputStream(this.output).writeObject(message)
        }
    }

    @Throws(IOException::class)
    override fun close() {
        input.close()
        output.close()
    }
}