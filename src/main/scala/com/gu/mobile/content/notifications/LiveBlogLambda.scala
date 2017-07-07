package com.gu.mobile.content.notifications

import com.amazonaws.services.kinesis.clientlibrary.types.UserRecord
import com.amazonaws.services.kinesis.model.Record
import com.amazonaws.services.lambda.runtime.events.KinesisEvent
import com.gu.crier.model.event.v1.EventPayload
import com.gu.mobile.content.notifications.model.KeyEvent

import scala.collection.JavaConverters._
import scala.concurrent.Future

object LiveBlogLambda extends Lambda {

  def handler(event: KinesisEvent) {
    val rawRecord: List[Record] = event.getRecords.asScala.map(_.getKinesis).toList
    val userRecords = UserRecord.deaggregate(rawRecord.asJava)

    CapiEventProcessor.process(userRecords.asScala) { event =>
      event.payload.map {
        case EventPayload.Content(content) =>
          val tags = content.tags.map(_.id).toList
          if(tags.exists(_ == "tone/minutebyminute" )) {
            val maybeLastEvent = KeyEvent.fromContent(content)
            logDebug(s"Key events: id: ${content.id} events: $maybeLastEvent")
          }
          Future.successful(true)
        case _ => Future.successful(true)
      }.getOrElse(Future.successful(true))
    }
  }
}
