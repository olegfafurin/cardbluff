package ru.drownshark.cardbluff

import java.io.*
import java.net.SocketException
import java.util.ArrayList

class StreamWorker(var input: InputStream, var output: OutputStream, var id: Int = -1) : Runnable, Closeable {

    private val listeners = ArrayList<MessageListener>()

    private val outputLock = Any()
    private val listenerLock = Any()

    private var stoppedEspecially = false

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
            if (e.message == "Connection reset" || e.message == "Socket closed") {
                synchronized(listenerLock) {
                    for (listener in listeners) {
                        listener.onGameEnded()
                    }
                }
            } else {
                synchronized(listenerLock) {
                    for (listener in listeners) {
                        listener.onException(e)
                    }
                }
            }
        } catch (e: EOFException) {
            if (!stoppedEspecially) {
                synchronized(listenerLock) {
                    for (listener in listeners) {
                        listener.onDisconnect(id)
                    }
                }
            }
        } catch (e: IOException) {
            synchronized(listenerLock) {
                for (listener in listeners) {
                    listener.onException(e)
                }
            }
        } catch (e: InterruptedException) {
            synchronized(listenerLock) {
                for (listener in listeners) {
                    listener.onDisconnect(id)
                }
            }
        }
        close()
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
        stoppedEspecially = true
        output.close()
        input.close()
    }
}