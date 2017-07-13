package com.gu.mobile.content.notifications.model

import com.gu.contentapi.client.model.v1.{ CapiDateTime, Content }
import com.gu.mobile.content.notifications.Logging
import com.gu.mobile.content.notifications.lib.NotificationsDynamoDb
import org.joda.time.DateTime

case class KeyEvent(blockId: String, title: Option[String], body: String, publishedDate: Option[DateTime], lastModified: Option[DateTime])

object KeyEvent extends Logging {

  implicit def maybeCapiDateTime2JodaDateTime(capiDateTime: Option[CapiDateTime]): Option[DateTime] = capiDateTime.map { d => new DateTime(d.dateTime) }

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
