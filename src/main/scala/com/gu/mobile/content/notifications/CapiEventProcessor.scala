package com.gu.mobile.content.notifications

import com.gu.crier.model.event.v1.Event
import com.gu.thrift.serializer.ThriftDeserializer
import software.amazon.kinesis.retrieval.KinesisClientRecord

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

object CapiEventProcessor extends Logging {

  def process(records: Seq[KinesisClientRecord])(sendNotification: Event => Future[Boolean])(implicit ec: ExecutionContext): Future[Int] = {
    val maybeNotificationsSent = records.map { record =>
      ThriftDeserializer.deserialize(record.data().array())(Event) match {
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
