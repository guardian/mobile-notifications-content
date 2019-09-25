package com.gu.mobile.content.notifications.lib

import java.net.URI

import com.gu.contentapi.client.model.v1._
import com.gu.mobile.content.notifications.lib.ContentApi._
import com.gu.mobile.content.notifications.lib.Seqs._
import com.gu.mobile.content.notifications.model.KeyEvent
import com.gu.mobile.content.notifications.{ Configuration, Logging }
import com.gu.mobile.notifications.client.models.TopicTypes.{ TagBlog, TagContributor, TagKeyword, TagSeries }
import com.gu.mobile.notifications.client.models._

import scala.util.Try

trait ContentAlertPayloadBuilder extends Logging {

  val Sender = "mobile-notifications-content"
  val config: Configuration

  private val topicsWithoutPrefix = Set(
    Topic(TagSeries, "football/series/the-uefa-euro-minute-2016"),
    Topic(TagSeries, "sport/series/the-olympic-games-minute-2016"),
    Topic(TagSeries, "membership/series/weekend-reading"),
    Topic(TagSeries, "membership/series/weekend-round-up"),
    Topic(TagSeries, "world/series/guardian-morning-briefing"),
    Topic(TagSeries, "politics/series/the-snap"),
    Topic(TagSeries, "us-news/series/the-campaign-minute-2016"),
    Topic(TagSeries, "australia-news/series/guardian-australia-s-morning-mail"),
    Topic(TagSeries, "us-news/series/guardian-us-briefing")
  )

  private val followableTopicTypes: Set[TagType] = Set(TagType.Series, TagType.Blog, TagType.Contributor)

  def buildPayLoad(content: Content): ContentAlertPayload = {
    val followableTag: Option[Tag] = content.tags.findOne(_.`type` == TagType.Series)
      .orElse(content.tags.findOne(_.`type` == TagType.Blog))
      .orElse(content.tags.findOne(_.`type` == TagType.Contributor))

    val topics = content.tags
      .filter(tag => followableTopicTypes.contains(tag.`type`))
      .flatMap(tagToTopic)
      .take(3)
      .toList

    ContentAlertPayload(
      title = contentTitle(content, followableTag, topics),
      message = Some(content.fields.flatMap { cf => cf.headline }.getOrElse(content.webTitle)),
      imageUrl = selectMainImage(content, minWidth = 750).map(new URI(_)),
      thumbnailUrl = content.thumbNail.map(new URI(_)),
      sender = Sender,
      link = getGuardianLink(content),
      importance = Importance.Major,
      topic = topics,
      debug = false,
      dryRun = None
    )
  }

  def buildPayLoad(content: Content, keyEvent: KeyEvent): ContentAlertPayload = {
    ContentAlertPayload(
      title = s"Liveblog update: ${keyEvent.title.getOrElse(content.webTitle)}",
      message = if (keyEvent.title.isDefined) Some(content.webTitle) else None,
      thumbnailUrl = content.thumbNail.map(new URI(_)),
      sender = Sender,
      link = getGuardianLink(content, Some(keyEvent)),
      importance = Importance.Major,
      topic = List(Topic(TopicTypes.Content, content.id)),
      debug = false,
      dryRun = None
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
    getTopicType(tag.`type`) map { tagType => Topic(tagType, tag.id) }
  }

  private def contentTitle(content: Content, followableTag: Option[Tag], topics: List[Topic]): String =
    if (topics.toSet.intersect(topicsWithoutPrefix).nonEmpty) {
      ""
    } else {
      followableTag.map { ft => ft.webTitle }.getOrElse("Following")
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

