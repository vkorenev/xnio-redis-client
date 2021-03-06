package xnioredis.codegen

import java.nio.file.{Path, Paths}
import java.util.Locale
import javax.lang.model.element.Modifier._

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.squareup.javapoet.TypeSpec.Builder
import com.squareup.javapoet._
import resource._
import xnioredis.Command
import xnioredis.commands._
import xnioredis.decoder.parser.{ArrayReplyParser, BulkStringReplyParser, IntegerReplyParser, SimpleStringReplyParser}
import xnioredis.encoder.{Encoder, MultiEncoder, MultiPairEncoder, RespArrayElementsWriter}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.io.Source

class Generator(dir: Path) {
  private val skippedGroups: Set[String] = Set("pubsub", "scripting", "transactions")
  private val supportedArgAttrs: Set[String] = Set("name", "type", "multiple", "optional")
  private val classLoader = Generator.getClass.getClassLoader

  def checkNotMandatory(args: List[Map[String, Any]]) = {
    val mandatoryArgs = args.filter(arg => isMandatory(arg) && !isMultiple(arg))
    if (mandatoryArgs.nonEmpty) {
      throw new Exception("Mandatory arguments are following optional: " + mandatoryArgs.mkString(", "))
    }
  }

  def generate() {
    val groupBuilders: mutable.Map[String, TypeSpec.Builder] = mutable.Map.empty
    val commandClassNames: mutable.Map[Int, ClassName] = mutable.Map(0 -> ClassName.get(classOf[Command[_]]))
    val bulkStringLiterals: mutable.Map[String, mutable.Map[String, FieldSpec]] = mutable.Map.empty
    val objectMapper = new ObjectMapper
    objectMapper.registerModule(DefaultScalaModule)

    def generateCommand(group: String, commandName: String, definition: Map[String, Any]): Any = {
      getReplyType(commandName) match {
        case Some(replyType) =>
          val arguments = definition.getOrElse("arguments", List.empty).asInstanceOf[List[Map[String, Any]]]
          val (mandatoryArguments, optionalArguments) = arguments.span(isMandatory)
          checkNotMandatory(optionalArguments)
          if (mandatoryArguments.forall(isArgumentSupported)) {
            val classBuilder: Builder = groupBuilders.getOrElseUpdate(group, {
              val className = typeName(group.split('_'))
              TypeSpec.classBuilder(className).addModifiers(PUBLIC).superclass(classOf[Commands])
            })
            val thisGroupLiterals = bulkStringLiterals.getOrElseUpdate(group, mutable.Map.empty)
            val nameConstants = commandName.split(' ') map { namePart =>
              thisGroupLiterals.getOrElseUpdate(namePart, {
                val fieldSpec = FieldSpec.builder(classOf[RespArrayElementsWriter], namePart.replace('-', '_'),
                  PRIVATE, STATIC, FINAL).initializer("new $T($S)", classOf[BulkStringLiteral], namePart).build()
                classBuilder.addField(fieldSpec)
                fieldSpec
              })
            }
            val summary = definition("summary").asInstanceOf[String]
            val methodSpec = buildMethodSpec(commandName, summary, replyType, mandatoryArguments, nameConstants)
            classBuilder.addMethod(methodSpec)
            if (optionalArguments.size == 1) {
              val optionalArg = optionalArguments.head
              if (isArgumentSupported(optionalArg)) {
                val methodSpec = buildMethodSpec(commandName, summary, replyType, mandatoryArguments :+ optionalArg, nameConstants)
                classBuilder.addMethod(methodSpec)
              } else {
                printf("Optional argument is not supported: %s\t(%s)\t%s\n", commandName, group, argsToString(arguments))
              }
            } else if (optionalArguments.size > 1) {
              printf("Some optional arguments are not supported: %s\t(%s)\t%s\n", commandName, group, argsToString(arguments))
            }
          } else {
            printf("Some arguments are not supported: %s\t(%s)\t%s\n", commandName, group, argsToString(arguments))
          }
        case None =>
          printf("Reply type unknown: %s\t(%s)\n", commandName, group)
      }
    }

    def getCommandClassName(argsNum: Int) = {
      commandClassNames.getOrElseUpdate(argsNum, {
        val className = ClassName.get("xnioredis.commands", "Command" + argsNum)
        val argTypeVars = Seq.tabulate(argsNum) {
          n => TypeVariableName.get("T" + (n + 1))
        }
        val replyTypeVar = TypeVariableName.get("R")
        val args = argTypeVars.zipWithIndex map {
          case (argTypeVar, n) => ParameterSpec.builder(argTypeVar, "arg" + (n + 1)).build()
        }
        val returnType = ParameterizedTypeName.get(ClassName.get(classOf[Command[_]]), replyTypeVar)
        val applyMethod = MethodSpec.methodBuilder("apply").addParameters(args.asJava)
            .addModifiers(PUBLIC, ABSTRACT).returns(returnType).build()
        val classSpec = TypeSpec.interfaceBuilder(className.simpleName()).addModifiers(PUBLIC)
            .addTypeVariables((argTypeVars :+ replyTypeVar).asJava).addMethod(applyMethod).build()
        val javaFile = JavaFile.builder(className.packageName(), classSpec).build()
        javaFile.writeTo(dir)
        className
      })
    }

    def getNamesAndTypes(arg: Map[String, Any]) = (arg("name"), arg("type")) match {
      case (n: String, t: String) => Seq((n, t))
      case (n: List[String@unchecked], t: List[String@unchecked]) => n zip t
    }

    def buildMethodSpec(commandName: String, summary: String, replyType: ClassName,
        arguments: Seq[Map[String, Any]], nameConstants: Traversable[FieldSpec]): MethodSpec = {
      val returnTypeVar = TypeVariableName.get("R")
      val replyParserParam = ParameterSpec.builder(
        ParameterizedTypeName.get(replyType, WildcardTypeName.subtypeOf(returnTypeVar)), "replyParser").build
      val methodName = varOrMethodName(commandName.toLowerCase(Locale.ENGLISH).split(Array(' ', '-')))
      val methodBuilder = MethodSpec.methodBuilder(methodName).addModifiers(PUBLIC, STATIC).addJavadoc(summary + '\n')
      val (argNames, typeVars, params) = arguments.map(argDef => {
        val multiple = isMultiple(argDef)
        val argNames = getNamesAndTypes(argDef).map(_._1)
        val argName = varOrMethodName(argNames.flatMap(_.split('-')))
        val typeVar = TypeVariableName.get(capitalize(argName))
        val encoderClass = ClassName.get((multiple, argNames.size) match {
          case (false, 1) => classOf[Encoder[_]]
          case (true, 1) => classOf[MultiEncoder[_]]
          case (true, 2) => classOf[MultiPairEncoder[_]]
          case _ => throw new IllegalStateException
        })
        val encoderParam = ParameterSpec.builder(
          ParameterizedTypeName.get(encoderClass, WildcardTypeName.supertypeOf(typeVar)), argName + "Encoder").build
        methodBuilder.addTypeVariable(typeVar).addParameter(encoderParam)
        (argName, typeVar, encoderParam)
      }).unzip3
      val argsNum = arguments.size
      val commandClassName = getCommandClassName(argsNum)
      val typeArguments = (0 until argsNum).map(typeVars(_)) :+ returnTypeVar
      val returnType: TypeName = ParameterizedTypeName.get(commandClassName, typeArguments: _*)

      methodBuilder.addTypeVariable(returnTypeVar).returns(returnType).addParameter(replyParserParam)
      val args = (argNames :+ replyParserParam) ++ nameConstants ++ params
      methodBuilder.addStatement(format(nameConstants.size, argsNum), args: _*)
      methodBuilder.build
    }

    val commands = managed(classLoader.getResourceAsStream("commands.json")) acquireAndGet {
      stream => objectMapper.readValue(stream, classOf[mutable.LinkedHashMap[String, Map[String, Any]]])
    }
    commands.foreach { case (commandName, definition) =>
      val group = definition("group").asInstanceOf[String]
      if (!skippedGroups.contains(group)) {
        try {
          generateCommand(group, commandName, definition)
        } catch {
          case e: Throwable =>
            Console.err.printf("Failed to generate command: %s\t(%s)\n", commandName, group)
            e.printStackTrace()
        }
      } else {
        printf("Group %s is not supported. Skipping %s\n", group, commandName)
      }
    }

    groupBuilders.values foreach { classBuilder =>
      val classSpec = classBuilder.build()
      val javaFile = JavaFile.builder("xnioredis.commands", classSpec).build()
      javaFile.writeTo(dir)
    }
  }

  private def getReplyType(commandName: String): Option[ClassName] = {
    val replyTypeStream = classLoader.getResourceAsStream("replies/" + commandName)
    if (replyTypeStream != null) {
      val replyType = managed(replyTypeStream) acquireAndGet {
        stream => Source.createBufferedSource(stream).mkString.trim
      }
      Some(ClassName.get(replyType match {
        case "array-reply" => classOf[ArrayReplyParser[_]]
        case "bulk-string-reply" => classOf[BulkStringReplyParser[_]]
        case "integer-reply" => classOf[IntegerReplyParser[_]]
        case "simple-string-reply" => classOf[SimpleStringReplyParser[_]]
      }))
    } else {
      None
    }
  }

  private def format(namePartsNum: Int, paramsNum: Int): String = {
    val sb = new StringBuilder("return ")
    if (paramsNum > 0) {
      for (i <- 1 to paramsNum) {
        sb.append(if (i == 1) "($" else ", $").append(i).append("L")
      }
      sb.append(") -> ")
    }

    sb.append("define($").append(paramsNum + 1).append("N")
    for (i <- 1 to namePartsNum) {
      sb.append(", $").append(paramsNum + 1 + i).append("N")
    }
    for (i <- 1 to paramsNum) {
      sb.append(", $").append(paramsNum + namePartsNum + 1 + i).append("N.encode($").append(i).append("L)")
    }
    sb.append(")")

    sb.toString
  }

  private def isMandatory(arg: Map[String, Any]): Boolean = arg.get("optional").isEmpty

  private def isMultiple(arg: Map[String, Any]): Boolean = arg.get("multiple").isDefined

  private def isArgumentSupported(arg: Map[String, Any]): Boolean = {
    arg.keySet.subsetOf(supportedArgAttrs) && (arg("name") match {
      case _: String => true
      case List(_: String, _: String) => isMultiple(arg)
      case _ => false
    })
  }

  private def capitalize(s: String): String = s.charAt(0).toUpper + s.substring(1)

  private def varOrMethodName(parts: Traversable[String]): String = {
    val sb = new StringBuilder(parts.head)
    for (part <- parts.tail) {
      sb.append(capitalize(part))
    }
    sb.toString
  }

  private def typeName(parts: Traversable[String]): String = parts.map(capitalize).mkString

  private def argsToString(args: List[Map[String, Any]]): String = {
    args.map(_.mkString("{", ", ", "}")).mkString("\n\t", ",\n\t", "")
  }
}

object Generator {
  def main(args: Array[String]) {
    if (args.length < 1) throw new IllegalArgumentException("Target directory must be provided as the first argument")
    new Generator(Paths.get(args(0))).generate()
  }
}
