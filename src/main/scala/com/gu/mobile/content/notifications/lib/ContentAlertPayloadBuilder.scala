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
    Topic(TagSeries, "world/series/guardian-morning-briefing"),
    Topic(TagSeries, "world/series/first-edition"),
    Topic(TagSeries, "australia-news/series/guardian-australia-s-morning-mail"),
    Topic(TagSeries, "us-news/series/guardian-us-briefing"),
    Topic(TagSeries, "politics/series/andrew-sparrows-election-briefing")
  )

  private val followableTopicTypes: Set[TagType] = Set(TagType.Series, TagType.Blog, TagType.Contributor)

  def buildPayLoad(content: Content): ContentAlertPayload = {
    val tagTypeSeries: Option[Tag] = content.tags.find(_.`type` == TagType.Series)
    val tagTypeBlog: Option[Tag] = content.tags
      .filterNot(tag => tag.id.contains("commentisfree/commentisfree"))
      .find(_.`type` == TagType.Blog)
    val tagTypeContributor: List[Tag] = content.tags.filter(_.`type` == TagType.Contributor).toList

    val followableTag: List[Tag] = (tagTypeSeries, tagTypeBlog, tagTypeContributor) match {
      case (Some(tagSeries), _, _) => List(tagSeries)
      case (None, Some(tagBlog), _) => List(tagBlog)
      case (None, None, tagContributor) => tagContributor.take(3)
    }

    val topics = content.tags
      .filter(tag => followableTopicTypes.contains(tag.`type`))
      .flatMap(tagToTopic)
      .take(3)
      .toList

    ContentAlertPayload(
      title = contentTitle(followableTag, topics),
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
    val seriesTag: Option[Tag] = content.tags.find(_.`type` == TagType.Series)
    val title: String = seriesTag.map(tag => s"Update: ${tag.webTitle}").getOrElse("Liveblog Update")
    ContentAlertPayload(
      title = Some(title),
      message = keyEvent.title,
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

  private def contentTitle(followableTag: List[Tag], topics: List[Topic]): Option[String] =
    if (topics.toSet.intersect(topicsWithoutPrefix).nonEmpty || followableTag.isEmpty) {
      None
    } else {
      Some(followableTag.map { ft => ft.webTitle }.mkString(", "))
    }

  private def selectMainImage(content: Content, minWidth: Int): Option[String] = {
    def width(asset: Asset): Int = asset.assetWidth.flatMap { aw => Try(aw.toInt).toOption }.getOrElse(0)
    def sortedAssets(element: Element): Seq[Asset] = element.assets.sortBy(width).toSeq

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

