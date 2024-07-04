package com.gu.mobile.content.notifications.lib

import com.gu.contentapi.client.model.v1.Content
import com.gu.crier.model.event.v1._
import com.gu.mobile.content.notifications.metrics.{ MetricDataPoint, Metrics }
import com.gu.mobile.content.notifications.{ CapiEventProcessor, Configuration }
import com.gu.mobile.notifications.client.models.ContentAlertPayload
import com.gu.thrift.serializer._
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ BeforeAndAfterEach, MustMatchers, OneInstancePerTest, WordSpecLike, Matchers => ShouldMatchers }
import org.scalatestplus.mockito.MockitoSugar
import software.amazon.kinesis.retrieval.KinesisClientRecord

import java.nio.ByteBuffer
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MessageSenderSpec extends MockitoSugar with WordSpecLike with MustMatchers with OneInstancePerTest with BeforeAndAfterEach with ScalaFutures {

  val config = new Configuration(true, "", "", "", "", "", "", "")
  val apiClient = mock[NotificationsApiClient]
  val content = mock[Content]
  val mockPayload = mock[ContentAlertPayload]
  val payloadBuilder = mock[ContentAlertPayloadBuilder]
  val metrics = mock[Metrics]

  val succesfulRight = Right(UUID.randomUUID())
  val successfulError = Left("")

  val captor = ArgumentCaptor.forClass(classOf[MetricDataPoint])

  "Message Sender" must {
    "record message success" in {
      val messageSender = new MessageSender(config, apiClient, payloadBuilder, metrics)
      when(payloadBuilder.buildPayLoad(content)) thenReturn mockPayload
      when(apiClient.send(mockPayload)) thenReturn succesfulRight
      messageSender.send(content)
      eventually {
        verify(metrics).send(captor.capture())
        captor.getValue.name mustEqual "SendNotificationLatency"
      }
    }

    "record message error" in {
      val messageSender = new MessageSender(config, apiClient, payloadBuilder, metrics)
      when(payloadBuilder.buildPayLoad(content)) thenReturn mockPayload
      when(apiClient.send(mockPayload)) thenReturn successfulError
      messageSender.send(content)
      eventually {
        verify(metrics).send(captor.capture())
        captor.getValue.name mustEqual "SendNotificationErrorLatency"
      }
    }

    val event = Event(
      payloadId = "1234567890",
      eventType = EventType.Update,
      itemType = ItemType.Tag,
      dateTime = 100000000L,
      payload = Some(EventPayload.RetrievableContent(RetrievableContent(
        id = "0987654321",
        capiUrl = "http://www.theguardian.com/",
        lastModifiedDate = Some(8888888888L),
        internalRevision = Some(444444)))))

    "properly deserialize a compressed event" in {
      val bytes = ThriftSerializer.serializeToBytes(event, Some(ZstdType), None)
      val record = KinesisClientRecord.builder().data(bytes).build()
      CapiEventProcessor.process(List(record))(event => Future.successful(true)).futureValue mustEqual 1
    }

    "properly deserialize a non-compressed event" in {
      val bytes = ThriftSerializer.serializeToBytes(event, None, None)
      val record = KinesisClientRecord.builder().data(bytes).build()
      CapiEventProcessor.process(List(record))(event => Future.successful(true)).futureValue mustEqual 1
    }
  }
}