package com.github.kperson.flyway

object Main extends App {

  (Option(System.getenv("LOCATION")), SecretsManager.cliSetting) match {
    case (Some(location), Some(settings)) =>
      Run.go(location, settings)
    case _ =>
      System.err.println("unable to parse configuration")
      System.exit(1)
  }

}
