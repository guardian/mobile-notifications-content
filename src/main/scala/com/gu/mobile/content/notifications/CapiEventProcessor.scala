package com.gu.mobile.content.notifications

import com.amazonaws.services.kinesis.model.Record
import com.gu.crier.model.event.v1.Event
import com.gu.thrift.serializer.ThriftDeserializer
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

object CapiEventProcessor extends Logging with ThriftDeserializer[Event] {

  val codec = Event

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def process(records: Seq[Record])(sendNotification: Event => Future[Boolean]) = {
    val maybeNotificationsSent = records.map { record =>
      logger.info("Reading record")
      eventFromRecord(record).flatMap(sendNotification).recover {
        case error =>
          logger.error(s"Failed to deserialize Kinesis record: ${error.getMessage}", error)
          false
      }
    }

    Future.sequence(maybeNotificationsSent).map {
      notificationsSent =>
        val notificationCount = notificationsSent.count(_ == true)
        logger.info(s"Sent $notificationCount notifications")
    }
  }

  private def eventFromRecord(record: Record): Future[Event] = {
    val buffer = record.getData.array
    deserialize(buffer, false).map { x =>
      logger.info("Success compressed!")
      x
    } recoverWith {
      case t: Throwable =>
        logger.error("Did not work", t)
        deserialize(buffer, true).map { x =>
          logger.info("Success uncompressed!")
          x
        }
    }
  }
}
