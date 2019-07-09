name := "CritHit"

version := "0.1"

scalaVersion := "2.13.0"

useJCenter := true

libraryDependencies ++= Seq(
  "net.dv8tion" % "JDA" % "3.8.3_464"
)

val stage = taskKey[Unit]("Stage heroku deploy")

val Stage = config("stage")

stage := {
  println("Now ready to run")
}