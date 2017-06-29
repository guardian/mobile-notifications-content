package com.gu.mobile.content.notifications

import com.amazonaws.services.kinesis.clientlibrary.types.UserRecord
import com.amazonaws.services.kinesis.model.Record
import com.amazonaws.services.lambda.runtime.events.KinesisEvent
import com.gu.contentapi.client.{ GuardianContentApiError, GuardianContentClient }
import com.gu.contentapi.client.model.ItemQuery
import com.gu.crier.model.event.v1.EventPayload.UnknownUnionField
import com.gu.crier.model.event.v1._
import com.gu.contentapi.client.model.v1.Content
import com.gu.mobile.content.notifications.lib.{ ContentAlertPayloadBuilder, MessageSender, NotificationsDynamoDb }
import com.gu.mobile.notifications.client.{ ApiClient => NotificiationsApiClient }
import com.gu.mobile.content.notifications.lib.{ ContentAlertPayloadBuilder, MessageSender }
import com.gu.mobile.content.notifications.lib.ContentApi._
import com.gu.mobile.content.notifications.lib.http.NotificationsHttpProvider
import com.gu.mobile.content.notifications.metrics.CloudWatchMetrics

import scala.collection.JavaConverters._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.Try
import scala.concurrent.duration._

sealed trait CapiResponse
case class CapiResponseSuccess(content: Content) extends CapiResponse
case class CapiResponseFailure(errorMsg: String) extends CapiResponse

object Lambda extends Logging {

  private val config = Config.load()
  private val payLoadBuilder = new ContentAlertPayloadBuilder {
    override val config: Config = Lambda.config
  }
  private val apiClient = NotificiationsApiClient(
    host = config.notificationsHost,
    apiKey = config.notificationsKey,
    httpProvider = NotificationsHttpProvider
  )

  val metrics = new CloudWatchMetrics(config)

  private val messageSender = new MessageSender(config, apiClient, payLoadBuilder, metrics)
  private val dynamo = NotificationsDynamoDb(config)
  private val capiClient = new GuardianContentClient(apiKey = config.contentApiKey)

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def handler(event: KinesisEvent) {
    val rawRecord: List[Record] = event.getRecords.asScala.map(_.getKinesis).toList
    val userRecords = UserRecord.deaggregate(rawRecord.asJava)

    CapiEventProcessor.process(userRecords.asScala) { event =>
      event.eventType match {
        case EventType.Update =>
          event.payload.map {
            case EventPayload.Content(content) =>
              logger.debug(s"Handle content update ${content.id}")
              val send = processContent(content)
              Future.successful(send)
            case EventPayload.RetrievableContent(content) =>
              logger.debug(s"Handle retrievable content or not: ${content.id}")
              handleRetrievableContent(content)
            case UnknownUnionField(e) =>
              logger.error(s"Unknown event payload $e. Consider updating capi models")
              Future.successful(false)
          }.getOrElse(Future.successful(false))
        case _ =>
          logger.error("Received non-updatable event type")
          Future.successful(false)
      }
    }
  }

  private def processContent(content: Content): Boolean = {
    logger.info(s"Processing ContendId: ${content.id} Published at: ${content.getLoggablePublicationDate}")
    if (content.isRecent && content.followableTags.nonEmpty) {
      val haveSeen = dynamo.haveSeenContentItem(content.id)
      if (haveSeen) {
        logger.info(s"Ignoring duplicate content ${content.id}")
      } else {
        logger.info(s"Sending notification for: ${content.id}")
        try {
          messageSender.send(content)
          dynamo.saveContentItem(content.id)
        } catch {
          case e: Exception =>
            logger.error(s"Unable to send notification for ${content.id}", e)
        }
      }
      !haveSeen
    } else {
      if (!content.isRecent) {
        logger.info(s"Ignoring older content ${content.id}")
      } else {
        logger.info(s"Ignoring content ${content.id} as it doesn't contain followable tags: ${content.tags}")
      }
      false
    }
  }

  private def handleRetrievableContent(retrievableContent: RetrievableContent): Future[Boolean] = {
    retrieveContent(retrievableContent) map {
      case CapiResponseSuccess(content) => processContent(content)
      case CapiResponseFailure(errorMsg) =>
        logger.error(errorMsg)
        false
    }
  }

  private def retrieveContent(retrievableContent: RetrievableContent): Future[CapiResponse] = {
    val contentId = retrievableContent.id
    val itemQuery = ItemQuery(contentId)
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