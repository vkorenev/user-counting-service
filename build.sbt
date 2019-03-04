name := "user-counting-service"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.8"

scalacOptions += "-Ypartial-unification"

scalacOptions in Test += "-Yrangepos"

Test / fork := true

val fs2Version = "1.0.4"
val http4sVersion = "0.20.0-M6"
val specs2Version = "4.4.1"
val logbackVersion = "1.2.3"

libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-core" % fs2Version,
  "co.fs2" %% "fs2-io" % fs2Version,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.tpolecat" %% "doobie-core" % "0.6.0",
  "org.tpolecat" %% "doobie-h2" % "0.6.0",
  "eu.timepit" %% "fs2-cron-core" % "0.1.0",
  "org.specs2" %% "specs2-core" % specs2Version % Test,
  "ch.qos.logback" % "logback-classic" % logbackVersion
)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4")
