package com.gu.mobile.content.notifications.lib

import com.amazonaws.auth.{ AWSCredentialsProviderChain, STSAssumeRoleSessionCredentialsProvider }
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec
import com.amazonaws.services.dynamodbv2.document.{ DynamoDB, Item }
import com.gu.mobile.content.notifications.{ Config, NotificationsDebugLogger }
import org.joda.time.DateTime

class NotificationsDynamoDb(dynamoDB: DynamoDB, tableName: String) {

  val table = dynamoDB.getTable(tableName)

  def saveContentItem(contentId: String): Unit = {
    val expiry = DateTime.now().plusDays(1).getMillis / 1000 //Expiry should be an epoch value: http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/time-to-live-ttl-how-to.html
    table.putItem(new Item().withPrimaryKey("contentId", contentId).withDouble("expiry", expiry))
  }

  def haveSeenContentItem(contentId: String): Boolean = {
    val getItemSpec = new GetItemSpec().withPrimaryKey("contentId", contentId)
    Option(table.getItem(getItemSpec)).isDefined
  }
}

object NotificationsDynamoDb extends NotificationsDebugLogger {
  def apply(config: Config): NotificationsDynamoDb = {

    //Table is in the mobile aws account wheras the lambda runs in the capi account
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
