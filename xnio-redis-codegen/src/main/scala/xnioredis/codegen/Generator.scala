package xnioredis.codegen

import java.nio.file.{Path, Paths}
import javax.lang.model.element.Modifier._

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.squareup.javapoet._
import resource._
import xnioredis.Command
import xnioredis.commands._
import xnioredis.decoder.parser.{ArrayReplyParser, BulkStringReplyParser, IntegerReplyParser, SimpleStringReplyParser}
import xnioredis.encoder.{Encoder, MultiEncoder, MultiPairEncoder, RespArrayElementsWriter}

import scala.collection.mutable
import scala.io.Source

class Generator(dir: Path) {
  private val skippedGroups: Set[String] = Set("scripting", "transactions")
  private val supportedArgAttrs: Set[String] = Set("name", "type", "multiple", "optional")
  private val classLoader = Generator.getClass.getClassLoader

  def generate() {
    val groupBuilders: mutable.Map[String, TypeSpec.Builder] = mutable.Map.empty
    val bulkStringLiterals: mutable.Map[String, mutable.Map[String, FieldSpec]] = mutable.Map.empty
    val objectMapper = new ObjectMapper
    objectMapper.registerModule(DefaultScalaModule)
    val commands = managed(classLoader.getResourceAsStream("commands.json")) acquireAndGet {
      stream => objectMapper.readValue(stream, classOf[mutable.LinkedHashMap[String, Map[String, Any]]])
    }
    commands.foreach { case (commandName, definition) =>
      val arguments = definition.getOrElse("arguments", List.empty).asInstanceOf[List[Map[String, Any]]]
      val group = definition("group").asInstanceOf[String]
      if (skippedGroups.contains(group)) {
        printf("Group %s is not supported. Skipping %s\t%s\n", group, commandName, arguments.mkString(", "))
      } else {
        val mandatoryArguments = arguments.filter(_.get("optional").isEmpty)
        if (mandatoryArguments.size < 4 &&
            mandatoryArguments.forall(argDef => argDef.keySet.subsetOf(supportedArgAttrs) && isNameSupported(argDef("name")))) {
          val classBuilder: TypeSpec.Builder = groupBuilders.getOrElseUpdate(group, {
            val className = typeName(group.split("_"))
            TypeSpec.classBuilder(className).addModifiers(PUBLIC).superclass(classOf[Commands])
          })
          val thisGroupLiterals = bulkStringLiterals.getOrElseUpdate(group, mutable.Map.empty)
          val nameParts = commandName.split(" ")
          val nameConstants = nameParts.map(namePart => thisGroupLiterals.getOrElseUpdate(namePart, {
            val fieldSpec = FieldSpec.builder(classOf[RespArrayElementsWriter], namePart.replace('-', '_'),
              PRIVATE, STATIC, FINAL).initializer("new $T($S)", classOf[BulkStringLiteral], namePart).build()
            classBuilder.addField(fieldSpec)
            fieldSpec
          }))
          val summary = definition("summary").asInstanceOf[String]

          getReplyType(commandName) match {
            case Some(replyType) =>
              val methodSpec = buildMethodSpec(commandName, summary, replyType, mandatoryArguments, nameConstants)
              classBuilder.addMethod(methodSpec)
            case None =>
              printf("Reply type unknown: %s\t(%s)\t%s\n", commandName, group, arguments.mkString(", "))
          }
        } else {
          printf("Some arguments are not supported: %s\t(%s)\t%s\n", commandName, group, arguments.mkString(", "))
        }
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

  private def buildMethodSpec(commandName: String, summary: String, replyType: ClassName,
      mandatoryArguments: Seq[Map[String, Any]], nameConstants: Traversable[FieldSpec]): MethodSpec = {
    val returnTypeVar = TypeVariableName.get("R")
    val replyParserParam = ParameterSpec.builder(
      ParameterizedTypeName.get(replyType, WildcardTypeName.subtypeOf(returnTypeVar)), "replyParser").build
    val methodName: String = commandName.replace(' ', '_').replace('-', '_').toLowerCase
    val methodBuilder = MethodSpec.methodBuilder(methodName).addModifiers(PUBLIC, STATIC).addJavadoc(summary + '\n')
    val (argNames, typeVars, params) = mandatoryArguments.map(argDef => {
      val multiple: Boolean = argDef.get("multiple") match {
        case Some(_) => true
        case None => false
      }
      val name = argDef("name")
      val (argName, isPair) = name match {
        case s: String =>
          (varOrMethodName(s.split("-")), false)
        case names: List[String] =>
          if (!multiple) throw new IllegalStateException
          if (names.size != 2) throw new IllegalStateException
          (varOrMethodName(names), true)
      }
      val argType = argDef.get("type")
      if (argType.isEmpty) {
        throw new IllegalStateException
      }
      val typeVar = TypeVariableName.get(capitalize(argName))
      val encoderClass = ClassName.get(
        if (multiple)
          if (isPair) classOf[MultiPairEncoder[_]]
          else classOf[MultiEncoder[_]]
        else classOf[Encoder[_]]
      )
      val encoderParam = ParameterSpec.builder(
        ParameterizedTypeName.get(encoderClass, WildcardTypeName.supertypeOf(typeVar)), argName + "Encoder").build
      methodBuilder.addTypeVariable(typeVar).addParameter(encoderParam)
      (argName, typeVar, encoderParam)
    }).unzip3
    val returnType: TypeName = mandatoryArguments.size match {
      case 0 => ParameterizedTypeName.get(ClassName.get(classOf[Command[_]]), returnTypeVar)
      case 1 => ParameterizedTypeName.get(ClassName.get(classOf[Command1[_, _]]), typeVars(0), returnTypeVar)
      case 2 => ParameterizedTypeName.get(ClassName.get(classOf[Command2[_, _, _]]), typeVars(0), typeVars(1), returnTypeVar)
      case 3 => ParameterizedTypeName.get(ClassName.get(classOf[Command3[_, _, _, _]]), typeVars(0), typeVars(1), typeVars(2), returnTypeVar)
      case _ => throw new IllegalStateException("name: " + commandName + ", args: " + mandatoryArguments.size)
    }

    methodBuilder.addTypeVariable(returnTypeVar).returns(returnType).addParameter(replyParserParam)
    val args = (argNames :+ replyParserParam) ++ nameConstants ++ params
    methodBuilder.addStatement(format(nameConstants.size, mandatoryArguments.size), args: _*)
    methodBuilder.build
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

  private def isNameSupported(name: Any): Boolean = {
    name.isInstanceOf[String] || name.isInstanceOf[List[_]] && name.asInstanceOf[List[_]].size == 2
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
}

object Generator {
  def main(args: Array[String]) {
    if (args.length < 1) throw new IllegalArgumentException("Target directory must be provided as the first argument")
    new Generator(Paths.get(args(0))).generate()
  }
}
