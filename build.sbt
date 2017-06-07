name := "mobile-content-notifications-lambda"

organization := "com.gu"

description:= "lambda to replace the content-notifications-service"

version := "1.0"

scalaVersion := "2.11.8"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-target:jvm-1.8",
  "-Ywarn-dead-code"
)

resolvers += Resolver.sonatypeRepo("releases")

assemblyMergeStrategy in assembly := {
  case "shared.thrift" => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

//hel//helo

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-sts" % "1.11.136",
  "com.amazonaws" % "amazon-kinesis-client" % "1.7.5",
  "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
  "com.amazonaws" % "aws-lambda-java-events" % "1.3.0",
  "com.amazonaws" % "aws-java-sdk-cloudwatch" % "1.11.136",
  "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.136",
  "com.gu" %% "content-api-client" % "11.14",
  "com.gu" %% "mobile-notifications-client" % "0.5.35",
  "io.spray" %%  "spray-json"  % "1.3.2",
  "org.apache.thrift" % "libthrift" % "0.9.1" force(),
  "com.twitter" %% "scrooge-core" % "4.6.0",
  "org.joda" % "joda-convert" % "1.8.1",
  "org.jsoup" % "jsoup" % "1.8.3",
  "com.typesafe.play" %% "play-json" % "2.3.2",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "org.mockito" % "mockito-all" % "1.9.0" % "test",
  "org.specs2" %% "specs2-core" % "3.8.6" % "test",
  "org.specs2" %% "specs2-matcher-extra" % "3.8.6" % "test"
)

enablePlugins(RiffRaffArtifact)

assemblyJarName := s"${name.value}.jar"
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cfn.yaml"), s"${name.value}-cfn/cfn.yaml")