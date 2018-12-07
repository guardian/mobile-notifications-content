package com.gu.mobile.content.notifications

import com.amazonaws.services.kinesis.model.Record
import com.gu.crier.model.event.v1.Event
import com.gu.thrift.serializer.ThriftDeserializer

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Try, Success, Failure }

object CapiEventProcessor extends Logging {

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def process(records: Seq[Record])(sendNotification: Event => Future[Boolean])(implicit ec: ExecutionContext): Future[Int] = {
    val maybeNotificationsSent = records.map { record =>
      ThriftDeserializer.deserialize(record.getData.array)(Event) match {
        case Success(event) => sendNotification(event)
        case Failure(error) =>
          logger.error(s"Failed to deserialize Kinesis record: ${error.getMessage}", error)
          Future.successful(false)
      }
    }

    Future.sequence(maybeNotificationsSent).map {
      notificationsSent =>
        val notificationCount = notificationsSent.count(_ == true)
        logger.info(s"Sent $notificationCount notifications")
        notificationCount
    }
  }
}
