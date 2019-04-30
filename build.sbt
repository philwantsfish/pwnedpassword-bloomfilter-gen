name := "com.PwnedPasswordBloomFilter"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.7" % "test",
  "com.google.guava" % "guava" % "27.1-jre",
  "com.github.scopt" %% "scopt" % "3.7.1",
)

trapExit := false
