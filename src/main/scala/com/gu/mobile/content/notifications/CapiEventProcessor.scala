package com.gu.mobile.content.notifications

import com.amazonaws.services.lambda.runtime.events.models.kinesis.Record
import com.gu.crier.model.event.v1.Event
import com.gu.thrift.serializer.ThriftDeserializer

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

object CapiEventProcessor extends Logging {

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
