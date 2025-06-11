package com.gu.mobile.content.notifications.metrics

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest
import org.mockito.Mockito._
import org.mockito.ArgumentCaptor
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.must.{ Matchers => ShouldMatchers }
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar
import org.specs2.specification.Scope

class MetricsActorSpec extends AnyWordSpecLike with MockitoSugar with ShouldMatchers {

  "The Metric Actor Logic" should {
    "not call cloudwatch if there is not data" in new MetricActorScope {
      actorLogic.aggregatePoint(Nil)
      verify(mockCloudWatch, times(0)).putMetricData(any[PutMetricDataRequest])
    }
    "call cloudwatch once if there's one namespace with less than 20 points" in new MetricActorScope {
      val metrics = List(
        new MetricDataPoint("test", "m1", 0d),
        new MetricDataPoint("test", "m1", 1d),
        new MetricDataPoint("test", "m1", 2d))

      actorLogic.aggregatePoint(metrics)
      val requestCaptor = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])
      verify(mockCloudWatch, times(1)).putMetricData(requestCaptor.capture())

      val metricData = requestCaptor.getValue.metricData()
      metricData must have size 1
      metricData.get(0).statisticValues.sampleCount mustEqual 3d

    }
    "call cloudwatch once but not aggregate if two metrics are received " in new MetricActorScope {
      val metrics = List(
        new MetricDataPoint("test", "m1", 0d),
        new MetricDataPoint("test", "m1", 1d),
        new MetricDataPoint("test", "m1", 2d),
        new MetricDataPoint("test", "m2", 5d),
        new MetricDataPoint("test", "m2", 6d))

      actorLogic.aggregatePoint(metrics)
      val requestCaptor = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])
      verify(mockCloudWatch, times(1)).putMetricData(requestCaptor.capture())

      val metricData = requestCaptor.getValue.metricData

      metricData must have size 2
      metricData.get(1).statisticValues.sampleCount mustEqual 3d
      metricData.get(0).statisticValues.sampleCount mustEqual 2d
    }
    "call cloudwatch once if there's more than one metric" in new MetricActorScope {
      val metrics = List(
        MetricDataPoint("test", "m1", 0d),
        MetricDataPoint("test", "m2", 1d),
        MetricDataPoint("test", "m3", 2d))
      actorLogic.aggregatePoint(metrics)
      verify(mockCloudWatch, times(1)).putMetricData(any[PutMetricDataRequest])
    }
    "call cloudwatch as many times as we have namespaces" in new MetricActorScope {
      val metrics = List(
        MetricDataPoint("namespace1", "m1", 0d),
        MetricDataPoint("namespace2", "m2", 1d),
        MetricDataPoint("namespace2", "m1", 2d))
      actorLogic.aggregatePoint(metrics)
      verify(mockCloudWatch, times(2)).putMetricData(any[PutMetricDataRequest])
    }
    "aggregate points into a MetricDatum" in new MetricActorScope {
      val metrics = List(
        MetricDataPoint("namespace1", "m1", 0d),
        MetricDataPoint("namespace1", "m1", 1d),
        MetricDataPoint("namespace1", "m1", 2d))
      actorLogic.aggregatePoint(metrics)
      val requestCaptor = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])
      verify(mockCloudWatch, times(1)).putMetricData(requestCaptor.capture())

      val metricDataList = requestCaptor.getValue.metricData
      metricDataList must have size 1
      val metricData = metricDataList.get(0)
      metricData.metricName mustEqual "m1"

      val statisticValues = metricData.statisticValues
      statisticValues.sum mustEqual 3d
      statisticValues.minimum mustEqual 0d
      statisticValues.maximum mustEqual 2d
      statisticValues.sampleCount mustEqual 3d
    }

    "aggregate points into batches if there are more than 20 metrics per namespace" in new MetricActorScope {
      val metrics = (1 to 21).toList.map { index =>
        MetricDataPoint("namespace1", s"m$index", index)
      }
      actorLogic.aggregatePoint(metrics)
      val requestCaptor = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])
      verify(mockCloudWatch, times(2)).putMetricData(any[PutMetricDataRequest])
    }

    "not aggregate points into multiple batches if there are 20 metrics or less per namespace" in new MetricActorScope {
      val metrics = (1 to 20).toList.map { index =>
        MetricDataPoint("namespace1", s"m$index", index)
      }
      actorLogic.aggregatePoint(metrics)
      val requestCaptor = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])
      verify(mockCloudWatch, times(1)).putMetricData(any[PutMetricDataRequest])
    }
  }

  trait MetricActorScope extends Scope {
    val mockCloudWatch = mock[CloudWatchClient]
    val actorLogic = new MetricActorLogic {
      override def cloudWatch: CloudWatchClient = mockCloudWatch
      override val stage: String = "CODE"
    }
  }

}
