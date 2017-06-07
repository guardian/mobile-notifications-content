package com.gu.mobile.content.notifications.lib

import com.amazonaws.auth.{ AWSCredentialsProviderChain, STSAssumeRoleSessionCredentialsProvider }
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec
import com.amazonaws.services.dynamodbv2.document.{ DynamoDB, Item }
import com.gu.mobile.content.notifications.{ Config, NotificationsDebugLogger }

class NotificationsDynamoDb(dynamoDB: DynamoDB, tableName: String) {

  val table = dynamoDB.getTable(tableName)

  def saveContentItem(contentId: String): Unit = {
    table.putItem(new Item().withPrimaryKey("contentId", contentId))
  }

  def haveSeenContentItem(contentId: String): Boolean = {
    val getItemSpec = new GetItemSpec().withPrimaryKey("contentId", contentId)
    Option(table.getItem(getItemSpec)).isDefined
  }
}

object NotificationsDynamoDb extends NotificationsDebugLogger {
  def apply(config: Config): NotificationsDynamoDb = {

    log(s"Configuring database access with cross acccount role: ${config.crossAccountDynamoRole} on table: ${config.contentDynamoTableName}")

    val dynamoCredentialsProvider = new AWSCredentialsProviderChain(
      new ProfileCredentialsProvider(),
      new STSAssumeRoleSessionCredentialsProvider.Builder(config.crossAccountDynamoRole, "mobile-db").build()
    )

    val client = AmazonDynamoDBClientBuilder.standard()
      .withRegion(Regions.EU_WEST_1)
      .withCredentials(dynamoCredentialsProvider)
      .build()

    val dynamoDB = new DynamoDB(client)

    new NotificationsDynamoDb(dynamoDB, config.contentDynamoTableName)
  }

}
