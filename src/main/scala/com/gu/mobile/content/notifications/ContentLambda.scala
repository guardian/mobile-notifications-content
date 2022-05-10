package com.gu.mobile.content.notifications

import com.gu.contentapi.client.model.v1.{Content, TagType}
import com.gu.mobile.content.notifications.lib.ContentApi._
import play.api.libs.json.{Format, Json}
import scalaj.http.Http

case class ExternalUserId (
  external_user_id: String
)

case class BrazeRequestBody (
  campaign_id: String,
  recipients: List[ExternalUserId]
)

object BrazeRequestBody {
 implicit val externalUserIdFormatJf: Format[ExternalUserId] = Json.format[ExternalUserId]
  implicit val brazeRequestBodyJF: Format[BrazeRequestBody] =Json.format[BrazeRequestBody]
}
object ContentLambda extends Lambda {

  def processContent(content: Content): Boolean = {
    logger.info(s"Processing ContendId: ${content.id} Published at: ${content.getLoggablePublicationDate}")
    if (content.isRecent && content.followableTags.nonEmpty) {
      val haveSeen = dynamo.haveSeenContentItem(content.id)
      if (haveSeen) {
        logger.info(s"Ignoring duplicate content ${content.id}")
      } else {
        try {
          messageSender.send(content)
          dynamo.saveContentItem(content.id)

          if (content.tags.exists(tag => tag.`type` == TagType.Contributor && tag.id == "profile/jayrayner" )) {
              val recipients = configuration.brazeExternalUserIdList.map(id => ExternalUserId(id))
              val brazeRequest = Json.toJson(BrazeRequestBody(configuration.brazeCampaignKey, recipients))

               val response = Http("https://rest.fra-01.braze.eu/campaigns/trigger/send")
                 .header("content-type", "application/json")
                 .header("authorization", s"Bearer ${configuration.brazeApiKey}")
                 .postData(brazeRequest.toString)
                 .asString
            if(response.code == 201) {
              logger.info("Braze call was successful")
            } else {
              logger.info("Braze call failed")
            }
          }

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
