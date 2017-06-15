package com.gu.mobile.content.notifications.lib

import com.amazonaws.services.cloudwatch.model.StandardUnit
import com.gu.contentapi.client.model.v1.Content
import com.gu.mobile.content.notifications.{ Config, NotificationsDebugLogger }
import com.gu.mobile.content.notifications.metrics.{ MetricDataPoint, Metrics }
import com.gu.mobile.notifications.client.ApiClient
import com.gu.mobile.notifications.client.models.{ ContentAlertPayload, NotificationPayload }

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

class MessageSender(config: Config, apiClient: ApiClient, payloadBuilder: ContentAlertPayloadBuilder, metrics: Metrics) extends NotificationsDebugLogger {

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  override val showDebug: Boolean = config.debug

  def send(content: Content): Unit = {
    sendNotification(payloadBuilder.buildPayLoad(content))
  }

  private def sendNotification(notification: Option[ContentAlertPayload]) {

    val start = System.currentTimeMillis()
    lazy val duration = System.currentTimeMillis() - start

    notification.foreach { n =>
      if (config.guardianNotificationsEnabled) {
        apiClient.send(n) onComplete {
          case Success(Right(_)) =>
            logDebug(s"Successfully sent notification for : ${n.title}")
            metrics.send(MetricDataPoint(name = "SendNotificationLatency", value = duration, unit = StandardUnit.Milliseconds))
          case Success(Left(error)) =>
            logDebug(s"Error sending notification: $n. error: ${error.description}  ")
            metrics.send(MetricDataPoint(name = "SendNotificationErrorLatency", value = duration, unit = StandardUnit.Milliseconds))
          case Failure(error) =>
            logDebug(s"Failed to send notification: $n. error: ${error.getMessage}")
            metrics.send(MetricDataPoint(name = "SendNotificationFailureLatency", value = duration, unit = StandardUnit.Milliseconds))
        }
      }
    }
  }
}
