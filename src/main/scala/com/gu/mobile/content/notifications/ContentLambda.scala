package com.gu.mobile.content.notifications

import com.amazonaws.services.kinesis.clientlibrary.types.UserRecord
import com.amazonaws.services.kinesis.model.Record
import com.amazonaws.services.lambda.runtime.events.KinesisEvent
import com.gu.contentapi.client.GuardianContentApiError
import com.gu.contentapi.client.model.ItemQuery
import com.gu.contentapi.client.model.v1.Content
import com.gu.crier.model.event.v1.EventPayload.UnknownUnionField
import com.gu.crier.model.event.v1.{ EventPayload, EventType, RetrievableContent }
import com.gu.mobile.content.notifications.lib.ContentApi._
import com.gu.mobile.content.notifications.lib.NotificationsDynamoDb

import scala.collection.JavaConverters._
import scala.concurrent.Future

object ContentLambda extends Lambda {

  private val dynamo = NotificationsDynamoDb(config)

  def contentHandler(event: KinesisEvent) {
    val rawRecord: List[Record] = event.getRecords.asScala.map(_.getKinesis).toList
    val userRecords = UserRecord.deaggregate(rawRecord.asJava)

    CapiEventProcessor.process(userRecords.asScala) { event =>
      event.eventType match {
        case EventType.Update =>
          event.payload.map {
            case EventPayload.Content(content) =>
              logger.info(s"Handle content update ${content.id}")
              val send = sendNotification(content)
              Future.successful(send)
            case EventPayload.RetrievableContent(content) =>
              logger.info(s"Handle retrievable content or not: ${content.id}")
              handleRetrievableContent(content)
            case UnknownUnionField(e) =>
              logger.info(s"Unknown event payload $e. Consider updating capi models")
              Future.successful(false)
        }.getOrElse(Future.successful(false))
        case _ =>
          logger.info("Received non-updatable event type")
          Future.successful(false)
      }
    }
  }

  private def sendNotification(content: Content): Boolean = {
    logger.info(s"Processing ContendId: ${content.id} Published at: ${content.getLoggablePublicationDate}")
    if (content.isRecent) {
      val haveSeen = dynamo.haveSeenContentItem(content.id)
      if (haveSeen) {
        logger.info(s"Ignoring duplicate piece of content ${content.id}")
      } else {
        logger.info(s"Sending notification for: ${content.id}")
        messageSender.send(content)
        dynamo.saveContentItem(content.id)
      }
      !haveSeen
    } else {
      logger.info(s"Ignoring older piece of content ${content.id}")
      false
    }
  }

  private def handleRetrievableContent(retrievableContent: RetrievableContent): Future[Boolean] = {
    retrieveContent(retrievableContent) map {
      case CapiResponseSuccess(content) => sendNotification(content)
      case CapiResponseFailure(errorMsg) =>
        log(errorMsg)
        false
    }
  }

  private def retrieveContent(retrievableContent: RetrievableContent): Future[CapiResponse] = {
    val contentId = retrievableContent.id
    val itemQuery = new ItemQuery(contentId)
      .showElements("all")
      .showFields("all")
      .showTags("all")

    capiClient.getResponse(itemQuery) map { itemResponse =>
      itemResponse.content match {
        case Some(content) => CapiResponseSuccess(content)
        case _ => CapiResponseFailure(s"Retrievable Content: No content found for $contentId")
      }
    } recover {
      case GuardianContentApiError(status, message, _) =>
        CapiResponseFailure(s"Retrievable Contenr: Recieved response from CAPI: $status with message: $message")
    }
  }
}