name := "mobile-content-notifications-lambda"

organization := "com.gu"

description:= "lambda to replace the content-notifications-service"

version := "1.0"

scalaVersion := "2.12.7"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-target:jvm-1.8",
  "-Ywarn-dead-code"
)

resolvers ++= Seq(
   Resolver.sonatypeRepo("releases"),
  "Guardian Platform Bintray" at "https://dl.bintray.com/guardian/platforms"
)

assemblyMergeStrategy in assembly := {
  case "shared.thrift" => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

val awsSdkVersion = "1.11.156"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-sts" % awsSdkVersion,
  "com.amazonaws" % "amazon-kinesis-client" % "1.7.6",
  "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
  "com.amazonaws" % "aws-lambda-java-events" % "1.3.0",
  "com.amazonaws" % "aws-java-sdk-cloudwatch" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-dynamodb" % awsSdkVersion,
  "com.gu" %% "content-api-client" % "11.55",
  "com.gu" %% "mobile-notifications-client" % "1.2",
  "org.apache.thrift" % "libthrift" % "0.9.1",
  "org.joda" % "joda-convert" % "1.8.1",
  "org.jsoup" % "jsoup" % "1.8.3",
  "com.typesafe.play" %% "play-json" % "2.6.9",
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  "com.typesafe.akka" %% "akka-actor" % "2.5.2",
  "com.squareup.okhttp3" % "okhttp" % "3.8.1",
  "com.gu" %% "simple-configuration-ssm" % "1.4.1",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "org.mockito" % "mockito-all" % "1.9.0" % "test",
  "org.specs2" %% "specs2-core" % "3.9.1" % "test",
  "org.specs2" %% "specs2-matcher-extra" % "3.9.1" % "test"
)

enablePlugins(RiffRaffArtifact)

assemblyJarName := s"${name.value}.jar"
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cfn.yaml"), s"${name.value}-cfn/cfn.yaml")