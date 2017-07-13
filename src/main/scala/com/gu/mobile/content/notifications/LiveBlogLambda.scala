package com.gu.mobile.content.notifications

import com.gu.contentapi.client.model.v1.Content
import com.gu.mobile.content.notifications.lib.ContentApi._
import com.gu.mobile.content.notifications.model.KeyEventProvider

object LiveBlogLambda extends Lambda {

  val keyEventProvider = new KeyEventProvider(dynamo)

  override def processContent(content: Content): Boolean = {
    if (content.isLive) {
      logger.info(s"Checking for new live updates to: ${content.id} .")
      keyEventProvider.getLatestKeyEvent(content).map {
        case (contentWithNewKeyEvent, keyEvent) =>
          logger.info(s"Found new key event for content: ${contentWithNewKeyEvent.id} block id: ${keyEvent.blockId}")
          messageSender.send(contentWithNewKeyEvent, Some(keyEvent))
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
