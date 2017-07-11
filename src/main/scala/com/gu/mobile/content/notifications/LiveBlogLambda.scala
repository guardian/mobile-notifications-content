package com.gu.mobile.content.notifications

import com.gu.contentapi.client.model.v1.Content
import com.gu.mobile.content.notifications.model.{ KeyEvent, KeyEventProvider }
import com.gu.mobile.content.notifications.lib.ContentApi._

object LiveBlogLambda extends Lambda {

  val keyEventProvider = new KeyEventProvider(dynamo)

  override def processContent(content: Content): Boolean = {
    val isLiveBlog = content.tags.map(_.id).toList.exists(_ == "tone/minutebyminute")
    val isLive = content.isLive
    logger.info(s"Checking for new live updates to: ${content.id} isLiveBlog: $isLiveBlog live now: $isLive")
    if (isLiveBlog && isLive) {
      keyEventProvider.getLatestKeyEvent(content).map {
        case (contentWithNewKeyEvent, keyEvent) =>
          logger.info(s"Found new key event for content: ${contentWithNewKeyEvent.id} block id: ${keyEvent.blockId}")
          messageSender.send(contentWithNewKeyEvent, Some(keyEvent))
          logger.info("++++ Message sent!")
          true
      }.getOrElse {
        logger.info(s"No new key event found for content: ${content.id}")
        false
      }
    } else {
      false
    }
  }
}
