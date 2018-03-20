package com.gu.mobile.content.notifications.lib

import com.gu.contentapi.client.model.v1.Content
import com.gu.mobile.content.notifications.LambdaConfig
import com.gu.mobile.content.notifications.metrics.{ MetricDataPoint, Metrics }
import com.gu.mobile.notifications.client.models.ContentAlertPayload
import com.gu.mobile.notifications.client.{ ApiClient, ApiHttpError }
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{ BeforeAndAfterEach, MustMatchers, OneInstancePerTest, WordSpecLike, Matchers => ShouldMatchers }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MessageSenderSpec extends MockitoSugar with WordSpecLike with MustMatchers with OneInstancePerTest with BeforeAndAfterEach {

  val config = new LambdaConfig(true, "", "", "", "", "", "", "")
  val apiClient = mock[ApiClient]
  val content = mock[Content]
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
  }
}
