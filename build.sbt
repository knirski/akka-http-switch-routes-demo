name := "akka-http-switch-routes-demo"

version := "0.1"

scalaVersion := "2.12.3"

val AkkaVersion = "2.5.4"

val AkkaHttpVersion = "10.0.10"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
)
