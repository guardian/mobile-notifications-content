package com.gu.mobile.content.notifications

import com.gu.contentapi.client.model.v1.Content
import com.gu.mobile.content.notifications.lib.ContentApi._

object ContentLambda extends Lambda {

  def processContent(content: Content): Boolean = {
    logger.info(s"Processing ContendId: ${content.id} Published at: ${content.getLoggablePublicationDate}")
    if (content.isRecent && content.followableTags.nonEmpty) {
      val haveSeen = false//dynamo.haveSeenContentItem(content.id)
      if (haveSeen) {
        logger.info(s"Ignoring duplicate content ${content.id}")
      } else {
        try {
          messageSender.send(content)
          dynamo.saveContentItem(content.id)
        } catch {
          case e: Exception =>
            logger.error(s"Unable to send notification for ${content.id}", e)
        }
      }
      !haveSeen
    } else {
      if (!content.isRecent) {
        logger.info(s"Ignoring older content ${content.id}")
      } else {
        logger.info(s"Ignoring content ${content.id} as it doesn't contain followable tags: ${content.tags}")
      }
      false
    }
  }
}