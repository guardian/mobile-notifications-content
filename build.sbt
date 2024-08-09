name := "mobile-notifications-content"

organization := "com.gu"

description:= "lambda to replace the content-notifications-service"

version := "1.0"

scalaVersion := "2.13.14"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-Ywarn-dead-code"
)

resolvers ++= Seq(
  "Guardian GitHub Releases" at "https://guardian.github.com/maven/repo-releases",
  "Guardian GitHub Snapshots" at "https://guardian.github.com/maven/repo-snapshots"
)

assembly / assemblyMergeStrategy := {
  case "META-INF/MANIFEST.MF" => MergeStrategy.discard
  case _ => MergeStrategy.first
}

val awsSdkVersion = "1.12.668"
val awsSdk2Version = "2.24.12"

libraryDependencies ++= Seq(
  "software.amazon.awssdk" % "sts" % awsSdk2Version,
  "software.amazon.awssdk" % "autoscaling" % awsSdk2Version,
  "software.amazon.awssdk" % "ec2" % awsSdk2Version,
  "software.amazon.awssdk" % "ssm" % awsSdk2Version,
  "software.amazon.kinesis" % "amazon-kinesis-client" % "2.6.0",
  "com.amazonaws" % "aws-java-sdk-sts" % awsSdkVersion,
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.3",
  "com.amazonaws" % "aws-lambda-java-events" % "3.11.4",
  "com.amazonaws" % "aws-java-sdk-cloudwatch" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-dynamodb" % awsSdkVersion,
  "com.gu" %% "content-api-client-default" % "19.4.1",
  "com.gu" %% "mobile-notifications-api-models" % "1.0.19",
  "com.gu" %% "thrift-serializer" % "5.0.7",
  "org.joda" % "joda-convert" % "1.9.2",
  "org.jsoup" % "jsoup" % "1.18.1",
  "org.slf4j" % "slf4j-simple" % "2.0.13",
  "org.slf4j" % "slf4j-api" % "2.0.13",
  "com.typesafe.akka" %% "akka-actor" % "2.5.24",
  "com.squareup.okhttp3" % "okhttp" % "3.14.9",
  "com.gu" %% "simple-configuration-ssm" % "1.7.0",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.17.2",
  "com.squareup.okhttp3" % "okhttp" % "4.12.0",
  "com.google.protobuf" % "protobuf-java" % "3.25.4",
  "org.json" % "json" % "20240303",
  "org.apache.commons" % "commons-compress" % "1.26.2",
  "org.scalatest" %% "scalatest" % "3.2.19" % Test,
  "org.mockito" % "mockito-all" % "1.10.19" % Test,
  "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % Test,
  "org.specs2" %% "specs2-core" % "4.20.4" % Test,
  "org.specs2" %% "specs2-matcher-extra" % "4.20.4" % Test
)
libraryDependencies += "com.github.luben" % "zstd-jni" % "1.5.5-11"

assemblyJarName := s"${name.value}.jar"
