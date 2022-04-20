package com.gu.mobile.content.notifications.metrics

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest
import org.mockito.Mockito._
import org.mockito.{ ArgumentCaptor, Matchers }
import org.scalatest.{ MustMatchers, WordSpecLike }
import org.scalatestplus.mockito.MockitoSugar
import org.specs2.specification.Scope

class MetricsActorSpec extends WordSpecLike with MockitoSugar with MustMatchers {

  "The Metric Actor Logic" should {
    "not call cloudwatch if there is not data" in new MetricActorScope {
      actorLogic.aggregatePoint(Nil)
      verify(mockCloudWatch, times(0)).putMetricData(Matchers.any[PutMetricDataRequest])
    }
    "call cloudwatch once if there's one namespace with less than 20 points" in new MetricActorScope {
      val metrics = List(
        new MetricDataPoint("test", "m1", 0d),
        new MetricDataPoint("test", "m1", 1d),
        new MetricDataPoint("test", "m1", 2d)
      )

      actorLogic.aggregatePoint(metrics)
      val requestCaptor = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])
      verify(mockCloudWatch, times(1)).putMetricData(requestCaptor.capture())

      val metricData = requestCaptor.getValue.getMetricData
      metricData must have size 1
      metricData.get(0).getStatisticValues.getSampleCount mustEqual 3d

    }
    "call cloudwatch once but not aggregate if two metrics are recieved " in new MetricActorScope {
      val metrics = List(
        new MetricDataPoint("test", "m1", 0d),
        new MetricDataPoint("test", "m1", 1d),
        new MetricDataPoint("test", "m1", 2d),
        new MetricDataPoint("test", "m2", 5d),
        new MetricDataPoint("test", "m2", 6d)
      )

      actorLogic.aggregatePoint(metrics)
      val requestCaptor = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])
      verify(mockCloudWatch, times(1)).putMetricData(requestCaptor.capture())

      val metricData = requestCaptor.getValue.getMetricData

      metricData must have size 2
      metricData.get(0).getStatisticValues.getSampleCount mustEqual 3d
      metricData.get(1).getStatisticValues.getSampleCount mustEqual 2d
    }
    "call cloudwatch once if there's more than one metric" in new MetricActorScope {
      val metrics = List(
        MetricDataPoint("test", "m1", 0d),
        MetricDataPoint("test", "m2", 1d),
        MetricDataPoint("test", "m3", 2d)
      )
      actorLogic.aggregatePoint(metrics)
      verify(mockCloudWatch, times(1)).putMetricData(Matchers.any[PutMetricDataRequest])
    }
    "call cloudwatch as many times as we have namespaces" in new MetricActorScope {
      val metrics = List(
        MetricDataPoint("namespace1", "m1", 0d),
        MetricDataPoint("namespace2", "m2", 1d),
        MetricDataPoint("namespace2", "m1", 2d)
      )
      actorLogic.aggregatePoint(metrics)
      verify(mockCloudWatch, times(2)).putMetricData(Matchers.any[PutMetricDataRequest])
    }
    "aggregate points into a MetricDatum" in new MetricActorScope {
      val metrics = List(
        MetricDataPoint("namespace1", "m1", 0d),
        MetricDataPoint("namespace1", "m1", 1d),
        MetricDataPoint("namespace1", "m1", 2d)
      )
      actorLogic.aggregatePoint(metrics)
      val requestCaptor = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])
      verify(mockCloudWatch, times(1)).putMetricData(requestCaptor.capture())

      val metricDataList = requestCaptor.getValue.getMetricData
      metricDataList must have size 1
      val metricData = metricDataList.get(0)
      metricData.getMetricName mustEqual "m1"

      val statisticValues = metricData.getStatisticValues
      statisticValues.getSum mustEqual 3d
      statisticValues.getMinimum mustEqual 0d
      statisticValues.getMaximum mustEqual 2d
      statisticValues.getSampleCount mustEqual 3d
    }

    "aggregate points into batches if there are more than 20 metrics per namespace" in new MetricActorScope {
      val metrics = (1 to 21).toList.map { index =>
        MetricDataPoint("namespace1", s"m$index", index)
      }
      actorLogic.aggregatePoint(metrics)
      val requestCaptor = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])
      verify(mockCloudWatch, times(2)).putMetricData(Matchers.any[PutMetricDataRequest])
    }

    "not aggregate points into multiple batches if there are 20 metrics or less per namespace" in new MetricActorScope {
      val metrics = (1 to 20).toList.map { index =>
        MetricDataPoint("namespace1", s"m$index", index)
      }
      actorLogic.aggregatePoint(metrics)
      val requestCaptor = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])
      verify(mockCloudWatch, times(1)).putMetricData(Matchers.any[PutMetricDataRequest])
    }
  }

  trait MetricActorScope extends Scope {
    val mockCloudWatch = mock[AmazonCloudWatch]
    val actorLogic = new MetricActorLogic {
      override def cloudWatch: AmazonCloudWatch = mockCloudWatch
      override val stage: String = "CODE"
    }
  }

}
