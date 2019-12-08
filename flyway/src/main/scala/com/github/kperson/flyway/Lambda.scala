package com.github.kperson.flyway

import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import java.io.{InputStream, OutputStream}
import java.nio.charset.StandardCharsets
import org.json4s._
import org.json4s.jackson.Serialization.read
import org.json4s.jackson.Serialization


class Lambda extends RequestStreamHandler {

  implicit val formats: Formats = Serialization.formats(NoTypeHints)

  def inputFromStr(str: String): Option[Settings] = {
    try {
      Some(read[Settings](str))
    }
    catch {
      case _: Throwable => None
    }
  }

  def secretsInputFromStr(str: String): Option[Settings] = {
    try {
      val arnInfo = read[SecretsManagerArn](str)
      SecretsManager.lambdaSettings(arnInfo.secretsManagerArn, arnInfo.db)
    }
    catch {
      case _: Throwable => None
    }
  }

  def settings(inputStream: InputStream): Option[Settings] = {
    val str = scala.io.Source.fromInputStream(inputStream).mkString
    (SecretsManager.cliSetting, inputFromStr(str), secretsInputFromStr(str)) match {
      case (Some(s), _, _) => Some(s)
      case (_, Some(s), _) => Some(s)
      case (_, _, Some(s)) => Some(s)
      case _ => None
    }
  }


  def handleRequest(input: InputStream, output: OutputStream, context: Context) {
    (settings(input), Option(System.getenv ("LAMBDA_TASK_ROOT"))) match {
      case (Some(s), Some(root)) =>
        val location =  root + "/sql"
        Run.go(location, s)
        output.write("{}".getBytes(StandardCharsets.UTF_8))
        output.flush()
        output.close()
      case _ => throw new RuntimeException("unable to parse configuration")
    }

  }

}
