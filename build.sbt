import com.typesafe.sbt.SbtStartScript

seq(SbtStartScript.startScriptForClassesSettings:_*)

name := "url-shortner"

version := "1.0"

scalaVersion := "2.10.5"

resolvers ++= Seq(
  "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
  "twttr" at "http://maven.twttr.com/"
)

libraryDependencies ++= Seq(
  "com.twitter" %% "finatra" % "1.6.0",
  "com.twitter" %% "finagle-stats" % "6.6.2",
  "com.top10" %% "scala-redis-client" % "1.16.0",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2.1" % "test"
)

