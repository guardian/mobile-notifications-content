package com.gu.mobile.content.notifications.lib

import com.amazonaws.auth.{ AWSCredentialsProviderChain, STSAssumeRoleSessionCredentialsProvider }
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec
import com.amazonaws.services.dynamodbv2.document.{ DynamoDB, Item }
import com.gu.mobile.content.notifications.{ Configuration, Logging }
import org.joda.time.DateTime

class NotificationsDynamoDb(dynamoDB: DynamoDB, config: Configuration) {

  val contentTable = dynamoDB.getTable(config.contentDynamoTableName)
  val liveBlogTable = dynamoDB.getTable(config.liveBlogContentDynamoTableName)

  def saveContentItem(contentId: String): Unit = {
    val expiry = DateTime.now().plusDays(1).getMillis / 1000 //Expiry should be an epoch value: http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/time-to-live-ttl-how-to.html
    contentTable.putItem(new Item().withPrimaryKey("contentId", contentId).withDouble("expiry", expiry.toDouble))
  }

  def haveSeenContentItem(contentId: String): Boolean = {
    val getItemSpec = new GetItemSpec().withPrimaryKey("contentId", contentId)
    Option(contentTable.getItem(getItemSpec)).isDefined
  }

  def haveSeenBlogEvent(contentId: String, blockId: String): Boolean = {
    val getItemSpec = new GetItemSpec().withPrimaryKey("contentId", contentId, "blockId", blockId)
    Option(liveBlogTable.getItem(getItemSpec)).isDefined
  }

  def saveLiveBlogEvent(contentId: String, blockId: String) = {
    val expiry = DateTime.now().plusDays(1).getMillis / 1000
    liveBlogTable.putItem(new Item().withPrimaryKey("contentId", contentId, "blockId", blockId).withDouble("expiry", expiry.toDouble))
  }
}

object NotificationsDynamoDb extends Logging {
  def apply(config: Configuration): NotificationsDynamoDb = {

    //Table is in the mobile aws account wheras the lambda runs in the capi account
    logger.info(s"Configuring database access with cross acccount role: ${config.crossAccountDynamoRole} on table: ${config.contentDynamoTableName}")

    val dynamoCredentialsProvider = new AWSCredentialsProviderChain(
      new ProfileCredentialsProvider(),
      new STSAssumeRoleSessionCredentialsProvider.Builder(config.crossAccountDynamoRole, "mobile-db").build())

    val client = AmazonDynamoDBClientBuilder.standard()
      .withRegion(Regions.EU_WEST_1)
      .withCredentials(dynamoCredentialsProvider)
      .build()

    val dynamoDB = new DynamoDB(client)

    new NotificationsDynamoDb(dynamoDB, config)
  }

}
