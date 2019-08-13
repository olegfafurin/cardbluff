package ru.drownshark.cardbluff

/**
 * Created by imd on 11/08/2019
 */

import java.lang.NullPointerException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException


class DBHelp(private val tableName: String) {

    /**
     * Create a new table in the test database
     *
     */

    fun createNewTable() {
        // SQLite connection string
        val url = "jdbc:sqlite:C:\\Users\\imd\\Documents\\playground\\cardbluff\\cardbluff.db"

        // SQL statement for creating a new table
        val sql = ("CREATE TABLE IF NOT EXISTS $tableName (\n"
                + "	id integer PRIMARY KEY,\n"
                + " login string NOT NULL, \n"
                + "	passwdHash string NOT NULL,\n"
                + "	rating real\n"
                + ");")

        try {
            DriverManager.getConnection(url).use { conn ->
                conn.createStatement().use { stmt ->
                    // create a new table
                    stmt.execute(sql)
                }
            }
        } catch (e: SQLException) {
            println(e.message)
        }

    }

    fun connect(): Connection? {
        // SQLite connection string
        val url = "jdbc:sqlite:C:\\Users\\imd\\Documents\\playground\\cardbluff\\cardbluff.db"
        var conn: Connection? = null
        try {
            conn = DriverManager.getConnection(url)
        } catch (e: SQLException) {
            println(e.message)
        }

        return conn
    }

    fun insert(login: String, passwdHash: String, rating: Double) {
        val sql = "INSERT INTO $tableName(login, passwdHash, rating) VALUES(?,?,?)";
        try {
            val conn = connect()!!
            val prst = conn.prepareStatement(sql)
            prst.setString(1, login)
            prst.setString(2, passwdHash)
            prst.setDouble(3, rating)
            prst.executeUpdate()
        } catch (e: SQLException) {
            println(e.message)
        }
    }

    fun getPasswordHash(requestedLogin: String): String? {
        val sql = "SELECT passwdHash FROM $tableName WHERE login='$requestedLogin'"

        try {
            this.connect().use { conn ->
                conn!!.createStatement().use { stmt ->
                    stmt.executeQuery(sql).use { rs ->
                        rs.next()
                        return rs.getString("passwdHash")
                    }
                }
            }
        } catch (e: SQLException) {
            println(e.message)
        }
        return null
    }

    fun getCurrentRating(requestedLogin: String): Double? {
        val sql = "SELECT rating FROM $tableName WHERE login='$requestedLogin'"

        try {
            this.connect().use { conn ->
                conn!!.createStatement().use { stmt ->
                    stmt.executeQuery(sql).use { rs ->
                        rs.next()
                        return rs.getDouble("rating")
                    }
                }
            }
        } catch (e: SQLException) {
            println(e.message)
        }
        return null
    }

    fun update(nickname: String, newRating: Double) {
        val sql = "UPDATE $tableName SET rating = '$newRating' WHERE login = '$nickname'"
        try {
            connect()!!.prepareStatement(sql).executeUpdate()
        } catch (e: NullPointerException) {
            println(e.message)
        }
    }

    fun selectAll() {
        val sql = "SELECT login, passwdHash, rating FROM $tableName"

        try {
            this.connect().use { conn ->
                conn!!.createStatement().use { stmt ->
                    stmt.executeQuery(sql).use { rs ->

                        // loop through the result set
                        while (rs.next()) {
                            println(
                                rs.getString("login") + "\t" +
                                        rs.getInt("passwdHash") + "\t" +
                                        rs.getDouble("rating")
                            )
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            println(e.message)
        }
    }


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val db = DBHelp("cardbluff")
            db.createNewTable()
//            db.selectAll()
//            db.insert("qwerty", "lol", -1.0)
//            println("${db.getPasswordHash("qwerty")} is the requested hash, ${db.getCurrentRating("qwerty")} is its rating")
//            db.update("qwerty", 100.0)
//            println("${db.getPasswordHash("qwerty")} is the requested hash, ${db.getCurrentRating("qwerty")} is its rating")
        }

    }
}