package com.gu.mobile.content.notifications.metrics

import akka.actor.{ ActorSystem, Props }
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import com.gu.mobile.content.notifications.{ Configuration, Logging }

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait Metrics {
  def send(mdp: MetricDataPoint): Unit
  def executionContext: ExecutionContext
}

class CloudWatchMetrics(config: Configuration) extends Metrics with Logging {

  private val actorSystem: ActorSystem = ActorSystem("MessageSending-timicMetric")

  implicit val executionContext: ExecutionContext = actorSystem.dispatcher
  logger.debug("Actor system created")

  private val cloudWatchClient = CloudWatchClient.create()

  val props = Props(new MetricsActor(cloudWatchClient, config))
  private val metricsActor = actorSystem.actorOf(props)
  logger.debug("Actor created")

  actorSystem.scheduler.schedule(
    initialDelay = 0.second,
    interval = 1.minute,
    receiver = metricsActor,
    message = MetricsActor.Aggregate)

  logger.debug("Actor scheduled")

  def send(mdp: MetricDataPoint): Unit = {
    logger.info(s"Sending metric: $mdp")

    metricsActor ! mdp
  }
}

case class MetricDataPoint(
  namespage: String = "mobile-notifications-lambda",
  name: String,
  value: Double,
  unit: StandardUnit = StandardUnit.NONE) {
  override def toString = s"Namespace: $namespage Name: $name value: $value"
}

