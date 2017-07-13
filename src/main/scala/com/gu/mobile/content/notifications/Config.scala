package com.gu.mobile.content.notifications

import java.util.Properties

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{ AWSCredentialsProviderChain, STSAssumeRoleSessionCredentialsProvider }
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder

import scala.util.Try

case class Config(
  guardianNotificationsEnabled: Boolean,
  notificationsHost: String,
  notificationsKey: String,
  contentApiKey: String,
  crossAccountDynamoRole: String,
  contentDynamoTableName: String,
  liveBlogContentDynamoTableName: String,
  stage: String
)

object Config extends Logging {
  val bucket = Option(System.getenv("ConfigurationBucket")).getOrElse(sys.error("No bucket containing configuration file provided. Lambda will not run"))
  val configurationKey = Option(System.getenv("ConfigurationKey")).getOrElse(sys.error("No filename for configuaration provided. Lambda will not run"))
  val configurationS3GetRole = Option(System.getenv("ConfigurationS3GetRole")).getOrElse(sys.error("No role to get configuration with. Lambda will not run"))
  val stage = Option(System.getenv("Stage")).getOrElse(sys.error("No Stage set. Lambda will not run"))

  //Reads configuration from a bucket in the mobile acccount
  val s3DestinationConfiguration = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider(),
    new STSAssumeRoleSessionCredentialsProvider.Builder(configurationS3GetRole, "mobile-s3").build()
  )

  val s3 = AmazonS3ClientBuilder.standard()
    .withRegion(Regions.EU_WEST_1)
    .withCredentials(s3DestinationConfiguration)
    .build()

  def load(): Config = {
    logger.info(s"Loading config. Bucket: $bucket Key: $configurationKey Stage: $stage")
    val properties = loadProperties(bucket, configurationKey) getOrElse sys.error("Could not load propeties from s3. Lambda will not run")

    val notificationsHost = getMandatoryProperty(properties, "notifications.host")
    logger.info(s"notifications.host: $notificationsHost")

    val notificationsKey = getMandatoryProperty(properties, "notifications.key")
    logger.info(s"notifications.key: $notificationsKey")

    val guardianNotificationsEnabled = getMandatoryProperty(properties, "notifications.enabled").toBoolean
    logger.info(s"notifications.enable $guardianNotificationsEnabled")

    val contentDynamoTableName = getMandatoryProperty(properties, "content.notifications.table")
    logger.info(s"content.notifications.table: $contentDynamoTableName")

    val crossAccountDynamoRole = getMandatoryProperty(properties, "content.notifications.crossAccountDynamoRole")
    logger.info(s"content.notifications.crossAccountDynamoRole: $crossAccountDynamoRole")

    val contentApiKey = getMandatoryProperty(properties, "content.api.key")
    logger.info(s"content.api.key $contentApiKey")

    val contentLiveBlogDynamoTableName = getMandatoryProperty(properties, "content.liveblog-notifications.table")
    logger.info(s"mobile-liveblog-content-notifications $contentLiveBlogDynamoTableName")

    Config(
      guardianNotificationsEnabled,
      notificationsHost,
      notificationsKey,
      contentApiKey,
      crossAccountDynamoRole,
      contentDynamoTableName,
      contentLiveBlogDynamoTableName,
      stage
    )
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
