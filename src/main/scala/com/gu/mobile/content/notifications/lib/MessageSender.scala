package com.gu.mobile.content.notifications.lib

import com.amazonaws.services.cloudwatch.model.StandardUnit
import com.gu.contentapi.client.model.v1.Content
import com.gu.mobile.content.notifications.{ Config, Logging }
import com.gu.mobile.content.notifications.metrics.{ MetricDataPoint, Metrics }
import com.gu.mobile.notifications.client.ApiClient
import com.gu.mobile.notifications.client.models.{ ContentAlertPayload, NotificationPayload }

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

class MessageSender(config: Config, apiClient: ApiClient, payloadBuilder: ContentAlertPayloadBuilder, metrics: Metrics) extends Logging {

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def send(content: Content): Unit = {
    sendNotification(payloadBuilder.buildPayLoad(content))
  }

  private def sendNotification(notification: ContentAlertPayload) {

    val start = System.currentTimeMillis()
    lazy val duration = System.currentTimeMillis() - start

    if (config.guardianNotificationsEnabled) {
      logger.info(s"Sending notification $notification")
      apiClient.send(notification) onComplete {
        case Success(Right(_)) =>
          logger.info(s"Successfully sent notification for : ${notification.title}")
          metrics.send(MetricDataPoint(name = "SendNotificationLatency", value = duration, unit = StandardUnit.Milliseconds))
        case Success(Left(error)) =>
          logger.error(s"Error sending notification: $notification. error: ${error.description}")
          metrics.send(MetricDataPoint(name = "SendNotificationErrorLatency", value = duration, unit = StandardUnit.Milliseconds))
        case Failure(error) =>
          logger.error(s"Failed to send notification: $notification. error: ${error.getMessage}", error)
          metrics.send(MetricDataPoint(name = "SendNotificationFailureLatency", value = duration, unit = StandardUnit.Milliseconds))
      }
    }
  }
}
