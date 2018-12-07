package com.gu.mobile.content.notifications.lib

import com.amazonaws.services.kinesis.model.Record
import com.gu.contentapi.client.model.v1.Content
import com.gu.crier.model.event.v1.{ Event, EventPayload, EventType, ItemType, RetrievableContent }
import com.gu.mobile.content.notifications.{ Configuration, CapiEventProcessor }
import com.gu.mobile.content.notifications.metrics.{ MetricDataPoint, Metrics }
import com.gu.mobile.notifications.client.models.ContentAlertPayload
import com.gu.mobile.notifications.client.{ ApiClient, ApiHttpError }
import com.gu.thrift.serializer._
import java.nio.ByteBuffer
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{ BeforeAndAfterEach, MustMatchers, OneInstancePerTest, WordSpecLike, Matchers => ShouldMatchers }
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MessageSenderSpec extends MockitoSugar with WordSpecLike with MustMatchers with OneInstancePerTest with BeforeAndAfterEach with ScalaFutures {

  val config = new Configuration(true, "", "", "", "", "", "", "")
  val apiClient = mock[ApiClient]
  val content = mock[Content]
  val contentFields = mock[ContentFields]
  val dateTime = CapiDateTime(1544178777001L, "")
  when(contentFields.firstPublicationDate) thenReturn (Some(dateTime))
  when(content.fields) thenReturn Some(contentFields)
  val mockPayload = mock[ContentAlertPayload]
  val payloadBuilder = mock[ContentAlertPayloadBuilder]
  val metrics = mock[Metrics]

  val succesfulRight = Future.successful(Right(()))
  val successfulError = Future.successful(Left(ApiHttpError(400)))
  val messageFailure = Future.failed(new IllegalStateException())

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

    "record message failure" in {
      val messageSender = new MessageSender(config, apiClient, payloadBuilder, metrics)
      when(payloadBuilder.buildPayLoad(content)) thenReturn mockPayload
      when(apiClient.send(mockPayload)) thenReturn messageFailure
      messageSender.send(content)
      eventually {
        verify(metrics).send(captor.capture())
        captor.getValue.name mustEqual "SendNotificationFailureLatency"
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
        internalRevision = Some(444444)
      )))
    )

    "properly deserialize a compressed event" in {
      val bytes = ThriftSerializer.serializeToBytes(event, Some(ZstdType), None)
      val record = new Record().withData(ByteBuffer.wrap(bytes))
      CapiEventProcessor.process(List(record))(event => Future.successful(true)).futureValue mustEqual 1
    }

    "properly deserialize a non-compressed event" in {
      val bytes = ThriftSerializer.serializeToBytes(event, None, None)
      val record = new Record().withData(ByteBuffer.wrap(bytes))
      CapiEventProcessor.process(List(record))(event => Future.successful(true)).futureValue mustEqual 1
    }
  }
}