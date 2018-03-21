package com.gu.mobile.content.notifications

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{ AWSCredentialsProviderChain, STSAssumeRoleSessionCredentialsProvider }
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.gu.conf.{ ConfigurationLoader, SSMConfigurationLocation }
import com.gu.{ AppIdentity, AwsIdentity }

case class LambdaConfig(
  guardianNotificationsEnabled: Boolean,
  notificationsHost: String,
  notificationsKey: String,
  contentApiKey: String,
  crossAccountDynamoRole: String,
  contentDynamoTableName: String,
  liveBlogContentDynamoTableName: String,
  stage: String
)

object LambdaConfig extends Logging {
  val appName = Option(System.getenv("App")).getOrElse(sys.error("No app name set. Lambda will not rum"))
  val stack = Option(System.getenv("Stack")).getOrElse(sys.error("Stack app name set. Lambda will not rum"))
  val stage = Option(System.getenv("Stage")).getOrElse(sys.error("Stage app name set. Lambda will not rum"))
  val crossAccountSsmReadingRole = Option(System.getenv("CrossAccountSsmReadingRole")).getOrElse(sys.error("No role to get configuration with. Lambda will not run"))

  val credentialsProvider = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider(),
    new STSAssumeRoleSessionCredentialsProvider.Builder(crossAccountSsmReadingRole, "mobile-ssm").build()
  )

  val conf = {
    val identity = AppIdentity.whoAmI(defaultAppName = appName)
    ConfigurationLoader.load(identity, credentialsProvider) {
      case AwsIdentity(app, stack, stage, _) =>
        val path = s"/$app/$stage/$stack"
        logger.info(s"Attempting to retrieve config from ssm with path: $path")
        SSMConfigurationLocation(path = path)
    }
  }

  def load(): LambdaConfig = {

    val notificationsHost = getMandatoryProperty("notifications.host")
    logger.info(s"notifications.host: $notificationsHost")

    val notificationsKey = getMandatoryProperty("notifications.key")
    logger.info(s"notifications.key: $notificationsKey")

    val guardianNotificationsEnabled = getMandatoryProperty("notifications.enabled").toBoolean
    logger.info(s"notifications.enable $guardianNotificationsEnabled")

    val contentDynamoTableName = getMandatoryProperty("content.notifications.table")
    logger.info(s"content.notifications.table: $contentDynamoTableName")

    val crossAccountDynamoRole = getMandatoryProperty("content.notifications.crossAccountDynamoRole")
    logger.info(s"content.notifications.crossAccountDynamoRole: $crossAccountDynamoRole")

    val contentApiKey = getMandatoryProperty("content.api.key")
    logger.info(s"content.api.key $contentApiKey")

    val contentLiveBlogDynamoTableName = getMandatoryProperty("content.liveblog-notifications.table")
    logger.info(s"mobile-liveblog-content-notifications $contentLiveBlogDynamoTableName")

    LambdaConfig(
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

  private def getMandatoryProperty(key: String) =
    Option(conf.getString(key)) getOrElse sys.error(s"Lamda unable to run - Mandatory property: '$key' missing")
}
