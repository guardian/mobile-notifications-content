package com.gu.mobile.content.notifications

import com.gu.contentapi.client.model.v1.Content
import com.gu.mobile.content.notifications.lib.ContentApi._

object ContentLambda extends Lambda {

  override def sendNotification(content: Content): Boolean = {
    logger.info(s"Processing ContendId: ${content.id} Published at: ${content.getLoggablePublicationDate}")
    if (content.isRecent) {
      val haveSeen = dynamo.haveSeenContentItem(content.id)
      if (haveSeen) {
        logger.info(s"Ignoring duplicate piece of content ${content.id}")
      } else {
        logger.info(s"Sending notification for: ${content.id}")
        messageSender.send(content)
        dynamo.saveContentItem(content.id)
      }
      !haveSeen
    } else {
      logger.info(s"Ignoring older piece of content ${content.id}")
      false
    }
  }
}