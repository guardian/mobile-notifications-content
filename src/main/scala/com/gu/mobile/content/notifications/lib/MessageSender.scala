package com.gu.mobile.content.notifications.lib

import com.amazonaws.services.cloudwatch.model.StandardUnit
import com.gu.contentapi.client.model.v1.Content
import com.gu.mobile.content.notifications.metrics.{ MetricDataPoint, Metrics }
import com.gu.mobile.content.notifications.model.KeyEvent
import com.gu.mobile.content.notifications.{ Configuration, Logging }
import com.gu.mobile.notifications.client.models.ContentAlertPayload

import scala.concurrent.ExecutionContext

class MessageSender(config: Configuration, apiClient: NotificationsApiClient, payloadBuilder: ContentAlertPayloadBuilder, metrics: Metrics) extends Logging {

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def send(content: Content, maybeKeyEvent: Option[KeyEvent] = None): Unit = {
    val payLoad = maybeKeyEvent match {
      case Some(keyEvent) =>
        payloadBuilder.buildPayLoad(content, keyEvent)
      case _ =>
        payloadBuilder.buildPayLoad(content)
    }

    sendNotification(payLoad)

  }

  private def sendNotification(notification: ContentAlertPayload): Unit = {

    val start = System.currentTimeMillis()
    lazy val duration = System.currentTimeMillis() - start

    if (config.guardianNotificationsEnabled) {
      logger.info(s"Sending notification $notification")
      apiClient.send(notification) match {
        case Right(id) =>
          logger.info(s"Successfully sent notification $id for : ${notification.title}")
          metrics.send(MetricDataPoint(name = "SendNotificationLatency", value = duration.toDouble, unit = StandardUnit.Milliseconds))
        case Left(error) =>
          logger.error(s"Error sending notification: $notification. error: $error")
          metrics.send(MetricDataPoint(name = "SendNotificationErrorLatency", value = duration.toDouble, unit = StandardUnit.Milliseconds))
      }
    }
  }
}
