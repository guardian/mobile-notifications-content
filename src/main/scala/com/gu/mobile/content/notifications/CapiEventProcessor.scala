package com.gu.mobile.content.notifications

import com.amazonaws.services.kinesis.model.Record
import com.gu.crier.model.event.v1.Event
import com.gu.mobile.content.notifications.CapiEventProcessor.eventFromRecord

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

object CapiEventProcessor extends NotificationsDebugLogger {

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def process(records: Seq[Record])(sendNotification: Event => Future[Boolean]) = {
    val map = records.flatMap { record =>
      val event = eventFromRecord(record)
      event.map {
        e => sendNotification(e)
      }.recover {
        case error =>
          log(s"Failed to deserialize Kinesis record: ${error.getMessage}")
          Future.successful(false)
      }.toOption
    }

    Future.sequence(map).map {
      results =>
        val notificationCount = results.count(_ == true)
        log(s"sent $notificationCount notifications")
    }
  }

  private def eventFromRecord(record: Record): Try[Event] = {
    val buffer = record.getData
    Try(ThriftDeserializer.fromByteBuffer(buffer)(Event.decoder))
  }
}
