package com.gu.mobile.content.notifications.model

import com.gu.contentapi.client.model.v1.{ CapiDateTime, Content }
import com.gu.contentapi.client.utils.CapiModelEnrichment.RichCapiDateTime

case class KeyEvent(id: String, title: Option[String], body: String, publishedDate: Option[CapiDateTime]) {
  //TODO - this is just for debugging, blat it later
  lazy val pTitle = title.getOrElse("**")
  lazy val pDate = publishedDate.map(_.toString()).getOrElse("**!**")
  override def toString = s"Key event. Id: ${id} title: ${pTitle}: publised $pDate"
}

object KeyEvent {

  def fromContent(content: Content): List[KeyEvent] =
    content.blocks
        .flatMap(_._2)
        .getOrElse(Nil)
        .filter(_.attributes.keyEvent.exists(identity))
        .filter(_.published)
        .map(block => KeyEvent(block.id, block.title, block.bodyTextSummary, block.publishedDate))
        .toList
        .reverse

}
