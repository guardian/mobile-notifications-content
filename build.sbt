name := "mobile-notifications-content"

organization := "com.gu"

description:= "lambda to replace the content-notifications-service"

version := "1.0"

scalaVersion := "2.13.16"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-Ywarn-dead-code",
  "-release:21"
)

resolvers ++= Seq(
  "Guardian GitHub Releases" at "https://guardian.github.com/maven/repo-releases",
  "Guardian GitHub Snapshots" at "https://guardian.github.com/maven/repo-snapshots"
)

assembly / assemblyMergeStrategy := {
  case "META-INF/MANIFEST.MF" => MergeStrategy.discard
  case _ => MergeStrategy.first
}

val awsSdkVersion = "2.31.62"

libraryDependencies ++= Seq(
  "software.amazon.awssdk" % "sts" % awsSdkVersion,
  "software.amazon.awssdk" % "autoscaling" % awsSdkVersion,
  "software.amazon.awssdk" % "ec2" % awsSdkVersion,
  "software.amazon.awssdk" % "ssm" % awsSdkVersion,
  "software.amazon.kinesis" % "amazon-kinesis-client" % "3.1.0",
  "software.amazon.awssdk" % "cloudwatch" % awsSdkVersion,
  "software.amazon.awssdk" % "dynamodb" % awsSdkVersion,
  "com.amazonaws" % "aws-lambda-java-core" % "1.3.0",
  "com.amazonaws" % "aws-lambda-java-events" % "3.16.0",
  "com.gu" %% "content-api-client-default" % "34.1.1",
  "com.gu" %% "mobile-notifications-api-models" % "3.0.0",
  "com.gu" %% "thrift-serializer" % "5.0.7",
  "org.joda" % "joda-convert" % "3.0.1",
  "org.jsoup" % "jsoup" % "1.20.1",
  "org.slf4j" % "slf4j-simple" % "2.0.17",
  "org.slf4j" % "slf4j-api" % "2.0.17",
  "com.typesafe.akka" %% "akka-actor" % "2.5.24",
  "com.squareup.okhttp3" % "okhttp" % "3.14.9",
  "com.gu" %% "simple-configuration-ssm" % "5.1.2",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.19.1",
  "com.squareup.okhttp3" % "okhttp" % "4.12.0",
  "com.google.protobuf" % "protobuf-java" % "4.31.1",
  "org.json" % "json" % "20250517",
  "org.apache.commons" % "commons-compress" % "1.27.1",
  "org.apache.avro" % "avro" % "1.12.0",
  "org.scalatest" %% "scalatest" % "3.2.19" % Test,
  "org.specs2" %% "specs2-core" % "4.21.0" % Test,
  "org.specs2" %% "specs2-matcher-extra" % "4.21.0" % Test,
  "org.mockito" % "mockito-core" % "5.18.0" % Test,
  "org.scalatestplus" %% "mockito-5-12" % "3.2.19.0" % Test,
)
libraryDependencies += "com.github.luben" % "zstd-jni" % "1.5.5-3"

assemblyJarName := s"${name.value}.jar"
