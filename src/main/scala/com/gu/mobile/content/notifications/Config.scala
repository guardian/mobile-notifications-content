package com.gu.mobile.content.notifications

import java.util.Properties

import com.amazonaws.auth.{ AWSCredentialsProviderChain, STSAssumeRoleSessionCredentialsProvider }
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{ AmazonS3Client, AmazonS3ClientBuilder }

import scala.reflect.macros.whitebox
import scala.util.Try

case class Config(
  guardianNotificationsEnabled: Boolean,
  notificationsHost: String,
  notificationsKey: String,
  contentApiKey: String,
  crossAccountDynamoRole: String,
  contentDynamoTableName: String,
  debug: Boolean = false
)

//Todo Refactor
object Config extends NotificationsDebugLogger {
  val bucket = Option(System.getenv("ConfigurationBucket")).getOrElse(sys.error("No bucket containing configuration file provided. Lambda will not run"))
  val configurationKey = Option(System.getenv("ConfigurationKey")).getOrElse(sys.error("No filename for configuaration provided. Lambda will not run"))
  val configurationS3GetRole = Option(System.getenv("ConfigurationS3GetRole")).getOrElse(sys.error("No role to get configuration with. Lambda will not run"))

  val s3DestinationConfiguration = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider(),
    new STSAssumeRoleSessionCredentialsProvider.Builder(configurationS3GetRole, "mobile-s3").build()
  )

  val s3 = AmazonS3ClientBuilder.standard()
    .withRegion(Regions.EU_WEST_1)
    .withCredentials(s3DestinationConfiguration)
    .build()

  def load(): Config = {
    log(s"Loading config. Bucket: $bucket Key: $configurationKey")
    val properties = loadProperties(bucket, configurationKey) getOrElse sys.error("Could not load propeties from s3. Lambda will not run")

    val notificationsHost = getMandatoryProperty(properties, "notifications.host")
    log(s"notifications.host: $notificationsHost")

    val notificationsKey = getMandatoryProperty(properties, "notifications.key")
    log(s"notifications.key: $notificationsKey")

    val guardianNotificationsEnabled = getMandatoryProperty(properties, "notifications.enabled").toBoolean
    log(s"notifications.enable $guardianNotificationsEnabled")

    val contentDynamoTableName = getMandatoryProperty(properties, "content.notifications.table")
    log(s"content.notifications.table: $contentDynamoTableName")

    val crossAccountDynamoRole = getMandatoryProperty(properties, "content.notifications.crossAccountDynamoRole")
    log(s"content.notifications.table: $contentDynamoTableName")

    val contentApiKey = getMandatoryProperty(properties, "content.api.key")
    log(s"content.api.key $contentApiKey")

    val debug = getProperty(properties, "debug").map(_.toBoolean).getOrElse(false)

    Config(guardianNotificationsEnabled, notificationsHost, notificationsKey, contentApiKey, crossAccountDynamoRole, contentDynamoTableName, debug)
  }

  private def loadProperties(bucket: String, key: String): Try[Properties] = {

    val inputStream = s3.getObject(bucket, key).getObjectContent
    val properties = new Properties()
    val result = Try(properties.load(inputStream)).map(_ => properties)
    inputStream.close()
    result
  }

  private def getProperty(properties: Properties, key: String) =
    Option(properties.getProperty(key))

  private def getMandatoryProperty(properties: Properties, key: String) =
    Option(properties.getProperty(key)) getOrElse sys.error(s"Lamda unable to run - Mandatory property: '$key' missing")
}
