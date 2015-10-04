package xnioredis.codegen

import java.net.{URL, URLConnection}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import resource._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.io.Source

object Updater extends App {
  val replyPattern = "@([a-zA-Z\\-]+)".r

  val objectMapper = new ObjectMapper
  objectMapper.registerModule(DefaultScalaModule)
  val classLoader: ClassLoader = Generator.getClass.getClassLoader
  val commands = managed(classLoader.getResourceAsStream("commands.json")) acquireAndGet { stream =>
    objectMapper.readValue(stream, classOf[mutable.LinkedHashMap[String, Map[String, Any]]])
  }
  val targetDir = Paths.get("src", "main", "resources", "replies")
  Files.createDirectories(targetDir)

  commands.foreach { case (commandName, definition) =>
    val url = new URL("https://raw.githubusercontent.com/antirez/redis-doc/master/commands/" +
      commandName.toLowerCase.replace(' ', '-') + ".md")
    println("Reading " + url)
    val urlConnection: URLConnection = url.openConnection
    val content = managed(urlConnection.getInputStream) acquireAndGet { inputStream =>
      Source.fromInputStream(inputStream).mkString
    }
    val tags = (for (m <- replyPattern findAllMatchIn content) yield m group 1).dropWhile {
      _ != "return"
    }.takeWhile {
      tag => tag != "examples" && tag != "history"
    }.filter {
      _ != "return"
    }.toIterable

    val file = targetDir.resolve(commandName)
    if (tags.size == 1) {
      Files.write(file, tags.asJava, StandardCharsets.UTF_8)
    }
  }
}
