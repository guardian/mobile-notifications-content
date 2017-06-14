package com.gu.mobile.content.notifications.metrics

import akka.actor.Actor
import com.amazonaws.services.cloudwatch.model._
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.gu.mobile.content.notifications.{ Config, NotificationsDebugLogger }

import scala.collection.JavaConversions._

class MetricsActor(val cloudWatch: AmazonCloudWatch, config: Config) extends Actor with MetricActorLogic {

  override val stage = config.stage
  override val showDebug: Boolean = config.debug

  var dataPoints = List.empty[MetricDataPoint] //This is fucked up dooode

  override def receive: Receive = {
    case metricDataPoint: MetricDataPoint =>
      logDebug("+++ Metrics actor: Recieved datapoint ")
      dataPoints = metricDataPoint :: dataPoints
    case MetricsActor.Aggregate =>
      logDebug("+++ Metrics actor: Recieved Aggregate ")
      aggregatePoint(dataPoints)
      dataPoints = Nil
  }
}

object MetricsActor {
  case object Aggregate
}

trait MetricActorLogic extends NotificationsDebugLogger {

  val stage: String
  def cloudWatch: AmazonCloudWatch

  def aggregatePointsPerMetric(metricPoints: List[MetricDataPoint], metricName: String): MetricDatum = {
    val (sum, min, max) = metricPoints.foldLeft((0d, Double.MaxValue, Double.MinValue)) {
      case ((aggSum, aggMin, aggMax), dataPoint) =>
        (aggSum + dataPoint.value, aggMin.min(dataPoint.value), aggMax.max(dataPoint.value))
    }

    val statSet = new StatisticSet
    statSet.setMaximum(max)
    statSet.setMinimum(min)
    statSet.setSum(sum)
    statSet.setSampleCount(metricPoints.size.toDouble)

    val unit = metricPoints.headOption.map(_.unit).getOrElse(StandardUnit.None)

    val metric = new MetricDatum()
    metric.setMetricName(metricName)
    metric.setUnit(unit)
    metric.setStatisticValues(statSet)

    metric

  }

  def aggregatePointsPerNamespaceMatches(points: List[MetricDataPoint]): List[(String, List[MetricDatum])] = {
    val pointsPerMetric = points.groupBy { point => (point.namespage, point.name) }.toList
    val allAwsMetrics = pointsPerMetric.map {
      case ((namespace, metricName), metricPoints) =>
        namespace -> aggregatePointsPerMetric(metricPoints, metricName)
    }

    val metricsPerNamespace = allAwsMetrics.foldLeft(Map.empty[String, List[MetricDatum]]) {
      case (aggregate, (namespace, awsPoint)) =>
        val points = aggregate.getOrElse(namespace, Nil)
        aggregate + (namespace -> (awsPoint :: points))
    }

    metricsPerNamespace.toList.flatMap {
      case (namespace, awsMetrics) =>
        val awsMetricsBatches = awsMetrics.grouped(20)
        awsMetricsBatches.map { batch => namespace -> batch }
    }
  }

  def aggregatePoint(points: List[MetricDataPoint]): Unit = {
    if (points.isEmpty) {
      logDebug(s"No metrics sent to cloudwatch")
    } else {

      logDebug(s"Sending metrics to cloudwatch")
      val metricsPerNameSpaceMatches = aggregatePointsPerNamespaceMatches(points)

      val metricsCount = metricsPerNameSpaceMatches.foldLeft(0) { case (sum, (_, batch)) => sum + batch.size }
      val batchesCount = metricsPerNameSpaceMatches.size
      val namespacesCount = metricsPerNameSpaceMatches.map(_._1).toSet

      try {
        metricsPerNameSpaceMatches.foreach {
          case (namespace, awsMetricsBatch) =>
            val metricRequest = new PutMetricDataRequest()
            metricRequest.setNamespace(s"$stage/$namespace")
            metricRequest.setMetricData(awsMetricsBatch)
            cloudWatch.putMetricData(metricRequest)
        }
        logDebug(s"Sent metrics to cloudwatch. " +
          s"Data points: ${points.size}, " +
          s"Metrics: $metricsCount, " +
          s"Namespaces: $namespacesCount, " +
          s"Batches: $batchesCount")
      } catch {
        case e: Exception => sys.error(s"Unable to send metrics to cloud ${e.getMessage}")
      }
    }
  }
}
