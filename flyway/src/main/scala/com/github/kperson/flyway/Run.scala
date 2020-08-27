package com.github.kperson.flyway

import org.flywaydb.core.Flyway

object Run {

  def go(location: String, settings: Settings) {
    println("starting migration")
    val flyway = Flyway.configure.dataSource(settings.jdbcURL, settings.dbUsername.orNull, settings.dbPassword.orNull).locations(s"filesystem:$location").load()
    try {
      flyway.migrate()
    }
    catch {
      case _: javax.net.ssl.SSLException => {

      }
    }
    println("migration completed")
  }

}