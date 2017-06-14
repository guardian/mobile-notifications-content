package com.gu.mobile.content.notifications.metrics

import akka.actor.{ ActorSystem, Props }
import com.amazonaws.regions.{ Region, Regions }
import com.amazonaws.services.cloudwatch.{ AmazonCloudWatchClient, AmazonCloudWatchClientBuilder }
import com.amazonaws.services.cloudwatch.model.StandardUnit
import com.gu.mobile.content.notifications.{ Config, NotificationsDebugLogger }

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait Metrics {
  def send(mdp: MetricDataPoint)
  def executionContext: ExecutionContext
}

class CloudWatchMetrics(config: Config) extends Metrics with NotificationsDebugLogger {

  override val showDebug: Boolean = config.debug

  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  private val actorSystem: ActorSystem = ActorSystem("MessageSending-timicMetric")
  private val cloudWatchClient = AmazonCloudWatchClientBuilder.defaultClient()

  val props = Props(new MetricsActor(cloudWatchClient, config))
  private val metricsActor = actorSystem.actorOf(props)

  actorSystem.scheduler.schedule(
    initialDelay = 0.second,
    interval = 1.minute,
    receiver = metricsActor,
    message = MetricsActor.Aggregate
  )

  def send(mdp: MetricDataPoint): Unit = {
    logDebug(s"Sending metric: $mdp")

    metricsActor ! mdp
  }
}

case class MetricDataPoint(
    namespage: String = "mobile-notifications-lambda",
    name: String,
    value: Double,
    unit: StandardUnit = StandardUnit.None
) {
  override def toString = s"Namespace: $namespage Name: $name value: $value"
}

