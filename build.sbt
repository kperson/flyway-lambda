lazy val commonSettings = Seq(
  organization := "com.github.kperson",
  version := "1.0.0",
  scalaVersion := "2.12.8",
  parallelExecution in Test := false,
  fork := true,
  test in assembly := {},
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case PathList("reference.conf") => MergeStrategy.discard
    case _ => MergeStrategy.first
  }
)


lazy val flyway = (project in file("flyway")).  settings(commonSettings: _*).
  settings(
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = true),
    libraryDependencies ++= Seq (
      "com.amazonaws"           % "aws-lambda-java-core"        % "1.2.0",
      "org.flywaydb"            % "flyway-maven-plugin"         % "5.2.4",
      "com.microsoft.sqlserver" % "mssql-jdbc"                  % "7.2.0.jre11",
      "mysql"                   % "mysql-connector-java"        % "8.0.12",
      "org.mariadb.jdbc"        % "mariadb-java-client"         % "2.4.0",
      "org.postgresql"          % "postgresql"                  % "42.2.5",
//      "net.sourceforge.jtds"    % "jtds"                        % "1.3.1",
//      "com.ibm.informix"        % "jdbc"                        % "4.10.10.0",
//      "com.h2database"          % "h2"                          % "1.4.197",
//      "org.hsqldb"              % "hsqldb"                      % "2.4.1",
//      "net.snowflake"           % "snowflake-jdbc"              % "3.6.23",
//      "org.firebirdsql.jdbc"    % "jaybird-jdk18"               % "3.0.5",
      "org.json4s"              %% "json4s-jackson"             % "3.6.5",
      "com.amazonaws"           % "aws-java-sdk-secretsmanager" % "1.11.688"
    ))

lazy val app = (project in file("app")).settings(commonSettings: _*).
  settings(
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = true),
  ).dependsOn(flyway)
