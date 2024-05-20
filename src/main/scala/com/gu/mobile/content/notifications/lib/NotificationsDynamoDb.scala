package com.gu.mobile.content.notifications.lib

import software.amazon.awssdk.regions.Region
import com.gu.mobile.content.notifications.{ Configuration, Logging }
import org.joda.time.DateTime
import software.amazon.awssdk.auth.credentials.{ AwsCredentialsProviderChain, ProfileCredentialsProvider }
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.{ AttributeValue, GetItemRequest, PutItemRequest }
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest

import scala.jdk.CollectionConverters._

class NotificationsDynamoDb(dynamoDB: DynamoDbClient, config: Configuration) {

  val contentTable = config.contentDynamoTableName
  val liveBlogTable = config.liveBlogContentDynamoTableName

  def saveContentItem(contentId: String): Unit = {
    val expiry = DateTime.now().plusDays(1).getMillis / 1000 //Expiry should be an epoch value: http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/time-to-live-ttl-how-to.html
    val request = PutItemRequest.builder()
      .tableName(contentTable)
      .item(Map(
        "contentId" -> AttributeValue.builder().s(contentId).build(),
        "expiry" -> AttributeValue.builder().n(expiry.toDouble.toString).build()).asJava)
      .build()
    dynamoDB.putItem(request)
  }

  def haveSeenContentItem(contentId: String): Boolean = {
    val getItemRequest = GetItemRequest.builder()
      .key(Map("contentId" -> AttributeValue.builder().s(contentId).build()).asJava)
      .tableName(contentTable).build()
    Option(dynamoDB.getItem(getItemRequest)).isDefined
  }

  def haveSeenBlogEvent(contentId: String, blockId: String): Boolean = {
    val getItemRequest = GetItemRequest.builder()
      .key(
        Map(
          "contentId" -> AttributeValue.builder().s(contentId).build(),
          "blockId" -> AttributeValue.builder().s(blockId).build()).asJava).tableName(liveBlogTable).build()
    Option(getItemRequest).isDefined
  }

  def saveLiveBlogEvent(contentId: String, blockId: String) = {
    val expiry = DateTime.now().plusDays(1).getMillis / 1000
    val request = PutItemRequest.builder()
      .tableName(contentTable)
      .item(Map(
        "contentId" -> AttributeValue.builder().s(contentId).build(),
        "blockId" -> AttributeValue.builder().s(blockId).build(),
        "expiry" -> AttributeValue.builder().n(expiry.toDouble.toString).build()).asJava)
      .build()
    dynamoDB.putItem(request)
  }
}

object NotificationsDynamoDb extends Logging {
  def apply(config: Configuration): NotificationsDynamoDb = {

    //Table is in the mobile aws account wheras the lambda runs in the capi account
    logger.info(s"Configuring database access with cross acccount role: ${config.crossAccountDynamoRole} on table: ${config.contentDynamoTableName}")
    val stsClient = StsClient
      .builder
      .region(Region.EU_WEST_1)
      .build

    val assumeRoleRequest = AssumeRoleRequest.builder
      .roleSessionName("mobile-db")
      .roleArn(config.crossAccountDynamoRole)
      .build

    val stsAssumeRoleCredentialsProvider = StsAssumeRoleCredentialsProvider
      .builder
      .stsClient(stsClient)
      .refreshRequest(assumeRoleRequest)
      .build

    val dynamoCredentialsProvider = AwsCredentialsProviderChain.of(
      ProfileCredentialsProvider.create(),
      stsAssumeRoleCredentialsProvider)

    val dynamoDBClient = DynamoDbClient
      .builder()
      .region(Region.EU_WEST_1)
      .credentialsProvider(dynamoCredentialsProvider)
      .build()

    new NotificationsDynamoDb(dynamoDBClient, config)
  }

}
