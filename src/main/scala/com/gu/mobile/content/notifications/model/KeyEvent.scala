package com.gu.mobile.content.notifications.model

import akka.event.Logging.LoggerException
import com.gu.contentapi.client.model.v1.{CapiDateTime, Content}
import com.gu.contentapi.client.utils.CapiModelEnrichment.RichCapiDateTime
import com.gu.mobile.content.notifications.Logging
import com.gu.mobile.content.notifications.lib.NotificationsDynamoDb

case class KeyEvent(blockId: String, title: Option[String], body: String, publishedDate: Option[CapiDateTime], lastModified: Option[CapiDateTime]) {
  //TODO - this is just for debugging, blat it later
  lazy val pTitle = title.getOrElse("**")
  lazy val pDate = publishedDate.map(_.toString()).getOrElse("**!**")
  override def toString = s"Key event. Id: ${blockId} title: ${pTitle}: publised $pDate"
}

object KeyEvent extends Logging {
  def fromContent(content: Content): Option[KeyEvent] = content.blocks
      .flatMap(_._2)
      .getOrElse(Nil)
      .filter(_.attributes.keyEvent.exists(identity))
      .filter(_.published)
      .map(block => KeyEvent(block.id, block.title, block.bodyTextSummary, block.publishedDate, block.lastModifiedDate))
      .toList
      .headOption
}

class KeyEventProvider(notificationsDynamoDb: NotificationsDynamoDb) extends Logging {

  def getLatestKeyEvent(content: Content): Option[(Content, KeyEvent)] = {
    KeyEvent.fromContent(content).flatMap { keyEvent =>
      notificationsDynamoDb.haveSeenBlogEvent(content.id, keyEvent.blockId) match {
        case true =>
          None
        case _ =>
          notificationsDynamoDb.saveLiveBlogEvent(content.id, keyEvent.blockId)
          Some((content, keyEvent))
      }
    }
  }
}
