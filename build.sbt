name := "hello-akka"

version := "0.1"

scalaVersion := "2.12.4"

resolvers += Resolver.url("Sbt-assembly-repo", url("https://bintray.com/eed3si9n/sbt-plugins/"))

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.7",
  "org.json4s" % "json4s-native_2.12" % "3.6.0-M1",
  "org.json4s" % "json4s-ext_2.12" % "3.6.0-M1",
  "org.scalikejdbc" %% "scalikejdbc" % "3.1.0",
  "mysql" % "mysql-connector-java" % "5.1.24"
)