package com.gu.mobile.content.notifications.lib

import com.gu.contentapi.client.model.v1.Content
import com.gu.mobile.content.notifications.Config
import com.gu.mobile.content.notifications.metrics.{ MetricDataPoint, Metrics }
import com.gu.mobile.notifications.client.models.ContentAlertPayload
import com.gu.mobile.notifications.client.{ ApiClient, ApiHttpError }
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{ BeforeAndAfterEach, MustMatchers, OneInstancePerTest, WordSpecLike, Matchers => ShouldMatchers }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MessageSenderSpec extends MockitoSugar with WordSpecLike with MustMatchers with OneInstancePerTest with BeforeAndAfterEach {

  val config = new Config(true, "", "", "", "", "", "", false)
  val apiClient = mock[ApiClient]
  val content = mock[Content]
  val mockPayload = mock[ContentAlertPayload]
  val payloadBuilder = mock[ContentAlertPayloadBuilder]
  val metrics = mock[Metrics]

  val succesfulRight = Future.successful(Right(()))
  val successfulError = Future.successful(Left(ApiHttpError(400)))
  val messageFailure = Future.failed(new IllegalStateException())

  "Message Sender" must {
    "record message success" ignore  {
      val messageSender = new MessageSender(config, apiClient, payloadBuilder, metrics)
      when(payloadBuilder.buildPayLoad(content)) thenReturn Some(mockPayload)
      when(apiClient.send(mockPayload)) thenReturn succesfulRight
      messageSender.send(content)
      eventually {
        verify(metrics, times(0)).send(Matchers.any[MetricDataPoint])
      }
    }

    "record message error" ignore {
      val messageSender = new MessageSender(config, apiClient, payloadBuilder, metrics)
      when(payloadBuilder.buildPayLoad(content)) thenReturn Some(mockPayload)
      when(apiClient.send(mockPayload)) thenReturn successfulError
      messageSender.send(content)
      eventually {
        verify(metrics, times(1)).send(Matchers.any[MetricDataPoint])
      }
    }

    "record message failure" ignore  {
      val messageSender = new MessageSender(config, apiClient, payloadBuilder, metrics)
      when(payloadBuilder.buildPayLoad(content)) thenReturn Some(mockPayload)
      when(apiClient.send(mockPayload)) thenReturn messageFailure
      messageSender.send(content)
      eventually {
        verify(metrics, times(1)).send(Matchers.any[MetricDataPoint])
      }
    }
  }
}
