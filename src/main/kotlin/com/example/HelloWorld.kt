package com.example

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder

import org.ktorm.database.Database
import org.ktorm.database.iterator
import org.ktorm.support.sqlite.SQLiteDialect

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

val dbFileName = "http4k-webgoat.db"

fun parseParams(name: String, msg: String): Map<String, String?> {
    val checkedName = name.takeUnless { it -> it.contains('\\') }?.ifBlank { "default_name" }
    val checkedMsg = msg.ifBlank { "default_msg" }
    return mapOf("parsed_name" to checkedName, "parsed_msg" to checkedMsg)
}

fun HelloWorld(db: Database): HttpHandler {
    return routes(
        "/ping" bind GET to { Response(OK).body("ok") },
        "/exec" bind GET to { req ->
            val cmd = req.query("cmd")
            if (cmd == "nop") {
              Response(OK).body("NOP")
            } else {
              // vulnerability: Remote Code Execution
              val proc = Runtime.getRuntime().exec(cmd)
              val lineReader = BufferedReader(InputStreamReader(proc.getInputStream()));
              val output = StringBuilder()
              lineReader.lines().forEach { line ->
                  output.append(line + "\n")
              }
              Response(OK).body("Did execute command `" + cmd + "`, got stdout:" + output)
            }
        },
        "/touch_file" bind GET to { req ->
          val name = req.query("name")
          val msg = req.query("msg")
          if (name == null || msg == null) {
            Response(OK).body("The `name` & `msg` parameters have to be set.")
          } else {
            val parsedParams = parseParams(name, msg)
            val fullPath = "/tmp/http4kexample/" + parsedParams["parsed_name"]
            val finalMsg = "MESSAGE: " + parsedParams["parsed_msg"]
            // vulnerability: Directory Traversal
            File(fullPath).writeText(finalMsg)
            Response(OK).body("Did write message `" + finalMsg + "` to file at `" + fullPath + "`")
          }
        },
        "add_user" bind GET to { req ->
            val username = req.query("username")
            val password = req.query("password")
            val out = db.useConnection {
                // vulnerability: SQL Injection
                val sql = "INSERT INTO user (username, password) VALUES ('$username', '$password');"
                val stmt = it.createStatement()
                stmt.execute(sql)
                username
            }
            Response(OK).body("Did insert new user `" + out + "`.")
        },
        "forgotten_debug_route" bind GET to { req ->
            val url = req.query("url")
            // vulnerability: Open Redirect
            Response(TEMPORARY_REDIRECT).header("Location", url)
        }
    )
}

fun main() {
    Files.deleteIfExists(Paths.get(dbFileName)) // remove db file on each application start

    val db = Database.connect(url = "jdbc:sqlite:$dbFileName", dialect = SQLiteDialect())
    db.useConnection { conn ->
        val createTableSQL = "CREATE TABLE IF NOT EXISTS user (id INTEGER PRIMARY KEY, username TEXT, password TEXT)"
        val createStmt = conn.createStatement()
        createStmt.execute(createTableSQL)

        val insertUserSQL = "INSERT INTO user (id, username, password) VALUES (1, 'admin', 'admin')"
        val insertUserStmt = conn.createStatement()
        insertUserStmt.execute(insertUserSQL)
    }

    val port = 8080
    println("Serving content on port " + port.toString() + ".")
    HelloWorld(db).asServer(SunHttp(port)).start()
}
