package com.github.kperson.flyway

import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import com.amazonaws.services.secretsmanager.model.{GetSecretValueRequest, ResourceNotFoundException}
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.read


case class Settings(jdbcURL: String, dbUsername: Option[String], dbPassword: Option[String])
case class SecretsManagerArn(secretsManagerArn: String, db: String)
case class SecretSettings(host: String, port: String, password: String, username: String)

object SecretsManager {

  def envSettings: Option[Settings] = {
    (Option(System.getenv("JDBC_URL")), Option(System.getenv("DB_USERNAME")), Option(System.getenv("DB_PASSWORD"))) match {
      case (Some(url), username, password) => Some(Settings(url, username, password))
      case _ => None
    }
  }

  def secretManagerSettings: Option[Settings] = {
    (Option(System.getenv("SECRETS_MANAGER_ARN")),  Option(System.getenv("DB"))) match {
      case (Some(arn), Some(db)) => lambdaSettings(arn, db)
      case _ => None
    }
  }

  def cliSetting: Option[Settings] = envSettings match {
    case Some(x) => Some(x)
    case _ => secretManagerSettings
  }

  def lambdaSettings(arn: String, dbName: String): Option[Settings] = {
    try {
      implicit val formats: Formats = Serialization.formats(NoTypeHints)
      val manager = AWSSecretsManagerClientBuilder.standard().build()
      val req = new GetSecretValueRequest()
      req.setSecretId(arn)
      val response = manager.getSecretValue(req)
      val s = read[SecretSettings](response.getSecretString)
      Some(Settings(s"jdbc:mysql://${s.host}:${s.port}/$dbName", Some(s.username), Some(s.password)))
    }
    catch {
      case _: ResourceNotFoundException => None
    }
  }


}