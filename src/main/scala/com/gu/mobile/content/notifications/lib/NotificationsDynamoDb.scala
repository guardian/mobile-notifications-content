package com.gu.mobile.content.notifications.lib

import software.amazon.awssdk.auth.credentials.{ AwsCredentialsProviderChain, EnvironmentVariableCredentialsProvider, ProfileCredentialsProvider }
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.{ AttributeValue, GetItemRequest, PutItemRequest }
import software.amazon.awssdk.regions.Region
import com.gu.mobile.content.notifications.{ Configuration, Logging }
import org.joda.time.DateTime
import scala.jdk.CollectionConverters._

class NotificationsDynamoDb(dynamoDB: DynamoDbClient, config: Configuration) {

  val client = DynamoDbClient.create()

  def saveContentItem(contentId: String): Unit = {
    val expiry = DateTime.now().plusDays(1).getMillis / 1000 //Expiry should be an epoch value: http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/time-to-live-ttl-how-to.html
    val item = Map(
      "contentId" -> AttributeValue.builder().s(contentId).build(),
      "expiry" -> AttributeValue.builder().n(expiry.toString).build())

    val request = PutItemRequest.builder()
      .tableName(config.contentDynamoTableName)
      .item(item.asJava)
      .build()

    client.putItem(request)
  }

  def haveSeenContentItem(contentId: String): Boolean = {
    val item = Map(
      "contentId" -> AttributeValue.builder().s(contentId).build())
    val request = GetItemRequest.builder()
      .tableName(config.contentDynamoTableName)
      .key(item.asJava)
      .build()

    Option(client.getItem(request)).isDefined
  }

  def haveSeenBlogEvent(contentId: String, blockId: String): Boolean = {
    val item = Map(
      "contentId" -> AttributeValue.builder().s(contentId).build(),
      "blockId" -> AttributeValue.builder().s(blockId).build())

    val request = GetItemRequest.builder()
      .tableName(config.liveBlogContentDynamoTableName)
      .key(item.asJava)
      .build()

    Option(request).isDefined
  }

  def saveLiveBlogEvent(contentId: String, blockId: String) = {
    val expiry = DateTime.now().plusDays(1).getMillis / 1000 //Expiry should be an epoch value: http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/time-to-live-ttl-how-to.html
    val item = Map(
      "contentId" -> AttributeValue.builder().s(contentId).build(),
      "blockId" -> AttributeValue.builder().s(blockId).build(),
      "expiry" -> AttributeValue.builder().n(expiry.toString).build())

    val request = PutItemRequest.builder()
      .tableName(config.contentDynamoTableName)
      .item(item.asJava)
      .build()

    client.putItem(request)
  }
}

object NotificationsDynamoDb extends Logging {
  def apply(config: Configuration): NotificationsDynamoDb = {

    //Table is in the mobile aws account wheras the lambda runs in the capi account
    logger.info(s"Configuring database access with cross acccount role: ${config.crossAccountDynamoRole} on table: ${config.contentDynamoTableName}")

  val req: AssumeRoleRequest = AssumeRoleRequest.builder
    .roleSessionName("mobile-db")
    .roleArn(config.crossAccountDynamoRole)
    .build()

  val credentialsProvider = AwsCredentialsProviderChain.of(
    ProfileCredentialsProvider.builder.profileName("mobile").build,
    StsAssumeRoleCredentialsProvider.builder
      .stsClient(StsClient.create)
      .refreshRequest(req)
      .build())

  val dynamoClient = DynamoDbClient.builder()
    .region(Region.EU_WEST_1)
    .credentialsProvider(credentialsProvider)
    .build()

    new NotificationsDynamoDb(dynamoClient, config)
  }
}
