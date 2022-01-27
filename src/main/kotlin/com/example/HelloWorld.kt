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

import java.io.File

fun parseParams(name: String, msg: String): Map<String, String?> {
    val checkedName = name.takeUnless { it -> it.contains('\\') }?.ifBlank { "default_name" }
    val checkedMsg = msg.ifBlank { "default_msg" }
    return mapOf("parsed_name" to checkedName, "parsed_msg" to checkedMsg)
}

fun HelloWorld(): HttpHandler {
    return routes(
        "/ping" bind GET to { Response(OK).body("ok") },
        "/exec" bind GET to { req ->
            val cmd = req.query("cmd")
            if (cmd == "nop") {
              Response(OK).body("NOP")
            } else {
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
            File(fullPath).writeText(finalMsg)
            Response(OK).body("Did write message `" + finalMsg + "` to file at `" + fullPath + "`")
          }
        }
    )
}

fun main() {
    val port = 8080
    println("Serving content on port " + port.toString() + ".")
    HelloWorld().asServer(SunHttp(port)).start()
}
