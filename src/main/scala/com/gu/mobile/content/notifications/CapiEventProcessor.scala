package com.gu.mobile.content.notifications

import com.amazonaws.services.kinesis.model.Record
import com.gu.crier.model.event.v1.Event

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

object CapiEventProcessor extends Logging {

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def process(records: Seq[Record])(sendNotification: Event => Future[Boolean]) = {
    val maybeNotificationsSent = records.flatMap { record =>
      val event = eventFromRecord(record)
      event.map {
        e => sendNotification(e)
      }.recover {
        case error =>
          logger.error(s"Failed to deserialize Kinesis record: ${error.getMessage}", error)
          Future.successful(false)
      }.toOption
    }

    Future.sequence(maybeNotificationsSent).map {
      notificationsSent =>
        val notificationCount = notificationsSent.count(_ == true)
        logger.info(s"Sent $notificationCount notifications")
    }
  }

  private def eventFromRecord(record: Record): Try[Event] = {
    val buffer = record.getData
    Try(ThriftDeserializer.fromByteBuffer(buffer, Event))
  }
}
