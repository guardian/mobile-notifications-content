package com.gu.mobile.content.notifications.lib

import java.net.URI

import com.gu.contentapi.client.model.v1._
import com.gu.mobile.content.notifications.lib.ContentApi._
import com.gu.mobile.content.notifications.lib.Seqs._
import com.gu.mobile.content.notifications.model.KeyEvent
import com.gu.mobile.content.notifications.{ Config, Logging }
import com.gu.mobile.notifications.client.models.TopicTypes.{ TagBlog, TagContributor, TagKeyword, TagSeries }
import com.gu.mobile.notifications.client.models._

import scala.util.Try

trait ContentAlertPayloadBuilder extends Logging {

  val Sender = "mobile-notifications-content"
  val config: Config

  private val topicsWithoutPrefix = Set(
    Topic(TagSeries, "football/series/the-uefa-euro-minute-2016"),
    Topic(TagSeries, "sport/series/the-olympic-games-minute-2016"),
    Topic(TagSeries, "membership/series/weekend-reading"),
    Topic(TagSeries, "membership/series/weekend-round-up"),
    Topic(TagSeries, "world/series/guardian-morning-briefing"),
    Topic(TagSeries, "politics/series/the-snap")
  )

  private val briefingTopic = Topic(TagSeries, "us-news/series/the-campaign-minute-2016")

  def buildPayLoad(content: Content): ContentAlertPayload = {
    val followableTag: Option[Tag] = content.tags.findOne(_.`type` == TagType.Series)
      .orElse(content.tags.findOne(_.`type` == TagType.Blog))
      .orElse(content.tags.findOne(_.`type` == TagType.Contributor))

    val topics = content.tags.flatMap(tagToTopic).take(20).toSet

    ContentAlertPayload(
      title = contentTitle(content, followableTag, topics),
      message = content.textStandFirst getOrElse content.webTitle,
      imageUrl = selectMainImage(content, minWidth = 750).map(new URI(_)),
      thumbnailUrl = content.thumbNail.map(new URI(_)),
      sender = Sender,
      link = getGuardianLink(content),
      importance = Importance.Major,
      topic = topics,
      debug = false
    )
  }

  def buildPayLoad(content: Content, keyEvent: KeyEvent): ContentAlertPayload = {
    ContentAlertPayload(
      title = s"Liveblog update: ${keyEvent.title.getOrElse(content.webTitle)}",
      message = if (keyEvent.title.isDefined) content.webTitle else "",
      thumbnailUrl = content.thumbNail.map(new URI(_)),
      sender = Sender,
      link = getGuardianLink(content, Some(keyEvent)),
      importance = Importance.Major,
      topic = Set(Topic(TopicTypes.Content, content.id)),
      debug = false
    )
  }

  private def getTopicType(tagType: TagType): Option[TopicType] = tagType match {
    case TagType.Contributor => Some(TagContributor)
    case TagType.Keyword => Some(TagKeyword)
    case TagType.Series => Some(TagSeries)
    case TagType.Blog => Some(TagBlog)
    case _ => None
  }

  private def tagToTopic(tag: Tag): Option[Topic] = {
    getTopicType(tag.`type`) map { maybeTagType => Topic(maybeTagType, tag.id) }
  }

  private def contentTitle(content: Content, followableTag: Option[Tag], topics: Set[Topic]): String = {
    def title = content.fields.flatMap { cf => cf.headline }.getOrElse(content.webTitle)
    def reason = followableTag.map { ft => ft.webTitle }.getOrElse("Following")

    if (topics.contains(briefingTopic)) {
      s"Got a minute? $title"
    } else if (topics.intersect(topicsWithoutPrefix).nonEmpty) {
      title
    } else {
      s"$reason: $title"
    }
  }

  private def selectMainImage(content: Content, minWidth: Int): Option[String] = {
    def width(asset: Asset): Int = asset.assetWidth.flatMap { aw => Try(aw.toInt).toOption }.getOrElse(0)
    def sortedAssets(element: Element): Seq[Asset] = element.assets.sortBy(width)

    val elements = content.elements.getOrElse(Nil)
    val mainImage = elements.find {
      e => e.`type` == ElementType.Image && e.relation == "main"
    }
    val selectedAsset = mainImage.flatMap { image => sortedAssets(image).find(asset => width(asset) >= minWidth) }
    selectedAsset.flatMap(_.file)
  }

  private def getGuardianLink(content: Content, keyEvent: Option[KeyEvent] = None) = GuardianLinkDetails(
    contentApiId = content.id,
    shortUrl = content.shortUrl,
    title = content.webTitle,
    thumbnail = content.thumbNail,
    git = GITContent,
    blockId = keyEvent.map { b => b.blockId }
  )
}

