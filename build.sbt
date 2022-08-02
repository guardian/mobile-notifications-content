name := "mobile-notifications-content"

organization := "com.gu"

description:= "lambda to replace the content-notifications-service"

version := "1.0"

scalaVersion := "2.13.5"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-target:jvm-1.8",
  "-Ywarn-dead-code"
)

resolvers ++= Seq(
  "Guardian GitHub Releases" at "https://guardian.github.com/maven/repo-releases",
  "Guardian GitHub Snapshots" at "https://guardian.github.com/maven/repo-snapshots"
)

assemblyMergeStrategy in assembly := {
  case "META-INF/MANIFEST.MF" => MergeStrategy.discard
  case _ => MergeStrategy.first
}

val awsSdkVersion = "1.11.772"

libraryDependencies ++= Seq(
  "software.amazon.awssdk" % "sts" % "2.17.109",
  "com.amazonaws" % "aws-java-sdk-sts" % awsSdkVersion,
  "com.amazonaws" % "amazon-kinesis-client" % "1.14.8",
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.1",
  "com.amazonaws" % "aws-lambda-java-events" % "2.2.8",
  "com.amazonaws" % "aws-java-sdk-cloudwatch" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-dynamodb" % awsSdkVersion,
  "com.gu" %% "content-api-client-default" % "17.21",
  "com.gu" %% "mobile-notifications-api-models" % "1.0.14",
  "com.gu" %% "thrift-serializer" % "4.0.2",
  "org.joda" % "joda-convert" % "1.8.1",
  "org.jsoup" % "jsoup" % "1.8.3",
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  "com.typesafe.akka" %% "akka-actor" % "2.5.24",
  "com.squareup.okhttp3" % "okhttp" % "3.14.8",
  "com.gu" %% "simple-configuration-ssm" % "1.5.6",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.12.6.1",
  "org.scalatest" %% "scalatest" % "3.0.8" % Test,
  "org.mockito" % "mockito-all" % "1.9.0" % Test,
  "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % Test,
  "org.specs2" %% "specs2-core" % "4.5.1" % Test,
  "org.specs2" %% "specs2-matcher-extra" % "4.5.1" % Test
)

enablePlugins(RiffRaffArtifact)

assemblyJarName := s"${name.value}.jar"
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffManifestProjectName := s"Mobile::${name.value}"
riffRaffArtifactResources += (file("cfn.yaml"), s"${name.value}-cfn/cfn.yaml")
