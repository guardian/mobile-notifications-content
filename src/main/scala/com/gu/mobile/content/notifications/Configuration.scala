package com.gu.mobile.content.notifications

import software.amazon.awssdk.regions.Region
import com.gu.conf.{ ConfigurationLoader, SSMConfigurationLocation }
import com.typesafe.config.Config
import com.gu.{ AppIdentity, AwsIdentity }
import software.amazon.awssdk.auth.credentials.{ AwsCredentialsProvider, AwsCredentialsProviderChain => AwsCredentialsProviderChainV2, ProfileCredentialsProvider => ProfileCredentialsProviderV2 }
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest

import scala.util.{ Failure, Success }

case class Configuration(
  guardianNotificationsEnabled: Boolean,
  notificationsHost: String,
  notificationsKey: String,
  contentApiKey: String,
  crossAccountDynamoRole: String,
  contentDynamoTableName: String,
  liveBlogContentDynamoTableName: String,
  stage: String)

object Configuration extends Logging {
  val appName = Option(System.getenv("App")).getOrElse(sys.error("No app name set. Lambda will not rum"))
  val stack = Option(System.getenv("Stack")).getOrElse(sys.error("Stack app name set. Lambda will not rum"))
  val stage = Option(System.getenv("Stage")).getOrElse(sys.error("Stage app name set. Lambda will not rum"))
  val crossAccountSsmReadingRole = Option(System.getenv("CrossAccountSsmReadingRole")).getOrElse(sys.error("No role to get configuration with. Lambda will not run"))

  logger.info(s"Cross account role: $crossAccountSsmReadingRole, Stack: $stack, Stage: $stage, App: $appName")

  val req: AssumeRoleRequest = AssumeRoleRequest.builder
    .roleSessionName("mobile-ssm")
    .roleArn(crossAccountSsmReadingRole)
    .build()

  def credentialsProvider: AwsCredentialsProvider = AwsCredentialsProviderChainV2.of(
    ProfileCredentialsProviderV2.builder.profileName("mobile").build,
    StsAssumeRoleCredentialsProvider.builder
      .stsClient(StsClient.create)
      .refreshRequest(req)
      .build())

  val conf = {
    (for {
      identity <- AppIdentity.whoAmI(defaultAppName = appName, credentials = credentialsProvider)
    } yield ConfigurationLoader.load(identity = identity, credentials = credentialsProvider) {
      case AwsIdentity(app, stack, stage, _) =>
        SSMConfigurationLocation(path = s"/$app/$stage/$stack", Region.EU_WEST_1.toString)
    }) match {
      case Success(c) => c
      case Failure(exception) => sys.error(s"Could not load config ${exception.getMessage}")
    }
  }

  def load(): Configuration = {

    val notificationsHost = getMandatoryProperty("notifications.host")
    logger.info(s"notifications.host: $notificationsHost")

    val notificationsKey = getMandatoryProperty("notifications.key")

    val guardianNotificationsEnabled = getMandatoryProperty("notifications.enabled").toBoolean
    logger.info(s"notifications.enable $guardianNotificationsEnabled")

    val contentDynamoTableName = getMandatoryProperty("content.notifications.table")
    logger.info(s"content.notifications.table: $contentDynamoTableName")

    val crossAccountDynamoRole = getMandatoryProperty("content.notifications.crossAccountDynamoRole")
    logger.info(s"content.notifications.crossAccountDynamoRole: $crossAccountDynamoRole")

    val contentApiKey = getMandatoryProperty("content.api.key")

    val contentLiveBlogDynamoTableName = getMandatoryProperty("content.liveblog-notifications.table")
    logger.info(s"mobile-liveblog-content-notifications $contentLiveBlogDynamoTableName")

    Configuration(
      guardianNotificationsEnabled,
      notificationsHost,
      notificationsKey,
      contentApiKey,
      crossAccountDynamoRole,
      contentDynamoTableName,
      contentLiveBlogDynamoTableName,
      stage)
  }

  private def getMandatoryProperty(key: String) =
    Option(conf.getString(key)) getOrElse sys.error(s"Lamda unable to run - Mandatory property: '$key' missing")
}
