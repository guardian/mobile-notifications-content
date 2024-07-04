package com.gu.mobile.content.notifications

import software.amazon.awssdk.services.kinesis.model.{ EncryptionType, Record }
import com.amazonaws.services.lambda.runtime.events.KinesisEvent
import com.gu.contentapi.client.model.{ ContentApiError, ItemQuery }
import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.client.GuardianContentClient
import com.gu.crier.model.event.v1.EventPayload.UnknownUnionField
import com.gu.crier.model.event.v1.{ EventPayload, RetrievableContent, _ }
import com.gu.mobile.content.notifications.lib.{ ContentAlertPayloadBuilder, MessageSender, NotificationsApiClient, NotificationsDynamoDb }
import com.gu.mobile.content.notifications.metrics.CloudWatchMetrics
import software.amazon.awssdk.core.SdkBytes
import software.amazon.kinesis.retrieval.{ AggregatorUtil, KinesisClientRecord }

import scala.jdk.CollectionConverters._
import scala.concurrent.{ ExecutionContext, Future }

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

/***
    As of version 3.0.0 of aws-lambda-java-events: https://github.com/aws/aws-lambda-java-libs/blob/main/aws-lambda-java-events/RELEASE.CHANGELOG.md#may-18-2020
    reading records from the lambda event returns a different model to the one that the kinesis client library deaggregation
    method is expecting.

    According to the amazon docs: https://docs.aws.amazon.com/streams/latest/dev/lambda-consumer.html we should be
    importing a custom deaggregation library for running in a lambda. However, the library is not officially
    supported by AWS and the version we require is not available on maven. See https://github.com/awslabs/kinesis-aggregation/issues/120

    So, manually creating an object of the type that the KCL library is expecting feels like the least worst option
   ***/
  def kinesisEventRecordToRecord(eventRecord: KinesisEvent.Record): KinesisClientRecord = {
    KinesisClientRecord.builder()
      .sequenceNumber(eventRecord.getSequenceNumber)
      .approximateArrivalTimestamp(eventRecord.getApproximateArrivalTimestamp.toInstant)
      .data(eventRecord.getData())
      .partitionKey(eventRecord.getPartitionKey)
      .encryptionType(EncryptionType.fromValue(eventRecord.getEncryptionType)).build()
  }
  def handler(event: KinesisEvent): Unit = {
    val eventRecords: List[KinesisEvent.Record] = event.getRecords.asScala.toList.map(_.getKinesis)
    val records = eventRecords.map(kinesisEventRecordToRecord)
    val aggregatorUtil = new AggregatorUtil()
    val userRecords: List[KinesisClientRecord] = aggregatorUtil.deaggregate(records.asJava).asScala.toList

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
            case EventPayload.Atom(atomAlias) =>
              logger.info(s"Unsupported content type: Atom, with id ${atomAlias.id}")
              Future.successful(false)
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
