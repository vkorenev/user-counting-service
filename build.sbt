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
val doobieVersion = "0.6.0"
val fs2cronVersion = "0.1.0"
val pureconfigVersion = "0.10.2"

libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-core" % fs2Version,
  "co.fs2" %% "fs2-io" % fs2Version,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat" %% "doobie-h2" % doobieVersion,
  "eu.timepit" %% "fs2-cron-core" % fs2cronVersion,
  "com.github.pureconfig" %% "pureconfig" % pureconfigVersion,
  "com.github.pureconfig" %% "pureconfig-cats-effect" % pureconfigVersion,
  "org.specs2" %% "specs2-core" % specs2Version % Test,
  "ch.qos.logback" % "logback-classic" % logbackVersion
)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4")
