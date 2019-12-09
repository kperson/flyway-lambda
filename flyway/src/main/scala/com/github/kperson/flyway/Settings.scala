package com.github.kperson.flyway

import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import com.amazonaws.services.secretsmanager.model.{GetSecretValueRequest, ResourceNotFoundException}
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.read


case class Settings(jdbcURL: String, dbUsername: Option[String], dbPassword: Option[String])
case class SecretsManagerArn(secretsManagerArn: String, db: Option[String])
case class SecretSettings(jdbcURL: Option[String], host: Option[String], port: Option[String], password: Option[String], username: Option[String])

object SecretsManager {

  def envSettings: Option[Settings] = {
    (Option(System.getenv("JDBC_URL")), Option(System.getenv("DB_USERNAME")), Option(System.getenv("DB_PASSWORD"))) match {
      case (Some(url), username, password) => Some(Settings(url, username, password))
      case _ => None
    }
  }

  def secretManagerSettings: Option[Settings] = {
    (Option(System.getenv("SECRETS_MANAGER_ARN")),  Option(System.getenv("DB"))) match {
      case (Some(arn), db) => lambdaSettings(arn, db)
      case _ => None
    }
  }

  def cliSetting: Option[Settings] = envSettings match {
    case Some(x) => Some(x)
    case _ => secretManagerSettings
  }

  def lambdaSettings(arn: String, dbName: Option[String]): Option[Settings] = {
    try {
      implicit val formats: Formats = Serialization.formats(NoTypeHints)
      val manager = AWSSecretsManagerClientBuilder.standard().build()
      val req = new GetSecretValueRequest()
      req.setSecretId(arn)
      val response = manager.getSecretValue(req)
      val s = read[SecretSettings](response.getSecretString)
      (s.jdbcURL, s.host, s.port, dbName) match {
        case (Some(url), _, _, _) => Some(Settings(url, s.username, s.password))
        case (_, Some(host), Some(port), db) => Some(Settings(s"jdbc:mysql://$host:$port/$db", s.username, s.password))
        case _ => None
      }
    }
    catch {
      case _: ResourceNotFoundException => None
    }
  }


}