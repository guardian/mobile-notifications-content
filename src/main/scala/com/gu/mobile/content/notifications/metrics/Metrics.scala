package com.gu.mobile.content.notifications.metrics

import com.amazonaws.services.cloudwatch.model.StandardUnit

trait Metrics {
  def send(mdp: MetricDataPoint)
  def sendError(mdp: MetricDataPoint)

}

class CloudWatchMetrics extends Metrics {
  val d = List.empty[MetricDataPoint]

  override def send(mdp: MetricDataPoint): Unit = {

  }

  override def sendError(mdp: MetricDataPoint): Unit = {

  }
}

case class MetricDataPoint(
  namespage: String = "mobile-notifications-lambda",
  name: String,
  value: Double,
  unit: StandardUnit = StandardUnit.None
)

