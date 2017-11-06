package com.github.dataanon.jdbc

import com.github.dataanon.Columns
import com.github.dataanon.Field
import com.github.dataanon.Record
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

class TableReader(protected val dbConfig: Map<String, Any>, protected val tableName: String, private val columns: Columns, private val whitelist: Array<String>) : Iterator<Record> {
    private var conn: Connection = DriverManager.getConnection(dbConfig["url"] as String, dbConfig["user"] as String, dbConfig["password"] as String)
    private var rs: ResultSet
    private var index = 0

    init {
        val stmt = conn.createStatement()
        val sql = "SELECT " +
                whitelist.joinToString(",") + "," + columns.names().joinToString(",") +
                " FROM " + tableName +
                (if(dbConfig.containsKey("limit")) " LIMIT ${dbConfig["limit"]} " else "")
        println(sql)
        rs = stmt.executeQuery(sql)
    }

    fun totalNoOfRecords(): Long {
        if (dbConfig.containsKey("limit")) return dbConfig["limit"] as Long

        val rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM $tableName")
        rs.next()
        val count = rs.getLong(1)
        rs.close()
        return count
    }

    override fun hasNext(): Boolean {
        val isNext = rs.next()
        if (!isNext) {
            rs.close()
            conn.close()
            return false
        }
        return isNext
    }

    override fun next(): Record {
        index++
        val fields = mutableListOf<Field>()
        columns.forEach { c ->
            fields.add(fieldFromResultSet(c.name))
        }
        whitelist.forEach { c ->
            fields.add(fieldFromResultSet(c))
        }
        return Record(fields, index)
    }

    private fun fieldFromResultSet(columnName: String): Field {
        val value = rs.getObject(columnName)
        return Field(columnName, value::class.toString(), value, value)
    }
}