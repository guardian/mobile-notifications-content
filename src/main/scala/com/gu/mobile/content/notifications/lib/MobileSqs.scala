package com.gu.mobile.content.notifications.lib

import com.amazonaws.auth.{AWSCredentialsProviderChain, STSAssumeRoleSessionCredentialsProvider}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSClientBuilder, model}
import com.amazonaws.services.sqs.model.SendMessageRequest
import com.gu.mobile.content.notifications.{Configuration, Logging}

class MobileSqs(client: AmazonSQS, config: Configuration) extends Logging {
  def sendMessage(contentId: String): Unit = {
    val msg = new SendMessageRequest()
      .withQueueUrl(config.sqsQueue)
      .withMessageBody(contentId)

    logger.info(s"About to send message to mobile SQS: ${msg.toString}")

    client.sendMessage(msg)
  }
}

object MobileSqs extends Logging {
    def apply(config: Configuration): MobileSqs = {
      logger.info(s"Configuring sqs permissions with cross account role: ${config.crossAccountSqsRole}")

      val credentialsProvider = new AWSCredentialsProviderChain(
        new ProfileCredentialsProvider(),
        new STSAssumeRoleSessionCredentialsProvider.Builder(config.crossAccountSqsRole, "mobile-sqs").build())

      val client = AmazonSQSClientBuilder.standard()
        .withRegion(Regions.EU_WEST_1)
        .withCredentials(credentialsProvider)
        .build()

      new MobileSqs(client, config)
    }
}
