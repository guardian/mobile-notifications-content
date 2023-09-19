package com.gu.mobile.content.notifications

import com.amazonaws.services.kinesis.clientlibrary.types.UserRecord
import com.amazonaws.services.kinesis.model.Record
import com.amazonaws.services.lambda.runtime.events.KinesisEvent
import com.gu.contentapi.client.model.{ContentApiError, ItemQuery}
import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.client.GuardianContentClient
import com.gu.crier.model.event.v1.EventPayload.UnknownUnionField
import com.gu.crier.model.event.v1.{EventPayload, RetrievableContent, _}
import com.gu.mobile.content.notifications.lib.{ContentAlertPayloadBuilder, MessageSender, NotificationsApiClient, NotificationsDynamoDb}
import com.gu.mobile.content.notifications.metrics.CloudWatchMetrics
import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSClientBuilder, model}
import com.amazonaws.services.sqs.model.SendMessageRequest

import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}

sealed trait CapiResponse
case class CapiResponseSuccess(content: Content) extends CapiResponse
case class CapiResponseFailure(errorMsg: String) extends CapiResponse

trait Lambda extends Logging {

  val configuration = Configuration.load()
  val payLoadBuilder = new ContentAlertPayloadBuilder {
    override val config: Configuration = configuration
  }

  val apiClient = new NotificationsApiClient(configuration)

  val metrics = new CloudWatchMetrics(configuration)
  val messageSender = new MessageSender(configuration, apiClient, payLoadBuilder, metrics)
  val dynamo = NotificationsDynamoDb(configuration)
  val capiClient = new GuardianContentClient(apiKey = configuration.contentApiKey)

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def handler(event: KinesisEvent): Unit = {
    val rawRecord: List[Record] = event.getRecords.asScala.map(_.getKinesis).toList
    val userRecords: List[UserRecord] = UserRecord.deaggregate(rawRecord.asJava).asScala.toList

    val sqs = AmazonSQSClientBuilder.defaultClient()
    val queueUrl = sqs.getQueueUrl("mobile-CODE-mobile-audio-generator-capiFirehoseEvents89AEA846-qnv7gNSYqGby").getQueueUrl
    logger.debug(s"Retrieved queue url $queueUrl")
    val msg = new SendMessageRequest()
      .withQueueUrl(queueUrl)
      .withMessageBody("test")
    logger.debug(s"About to send message ${msg.toString}")
    sqs.sendMessage(msg)

    CapiEventProcessor.process(userRecords) { event =>
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
            case _ =>
              logger.warn(s"Unknown event payload ${event.payload}. Consider updating capi models")
              Future.successful(false)
          }.getOrElse(Future.successful(false))
        case _ =>
          logger.info("Received non-updatable event type")
          Future.successful(false)
      }
    }
  }

  def processContent(content: Content): Boolean

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
      case ContentApiError(status, message, _) =>
        CapiResponseFailure(s"Retrievable Contenr: Recieved response from CAPI: $status with message: $message")
    }
  }

}
