package com.gu.mobile.content.notifications.lib

import java.net.URI

import com.gu.contentapi.client.model.v1._
import com.gu.mobile.content.notifications.Config
import com.gu.mobile.notifications.client.models.TopicTypes.{ TagBlog, TagContributor, TagKeyword, TagSeries }
import com.gu.mobile.notifications.client.models._
import org.joda.time.LocalDate
import org.mockito.Mockito._
import org.scalatest.{ MustMatchers, WordSpec, WordSpecLike }
import org.scalatest.mockito.MockitoSugar

class ContentAlertPayloadBuilderSpec extends MockitoSugar with WordSpecLike with MustMatchers {

  val conf = mock[Config]
  when(conf.debug).thenReturn(false)
  val builder = new ContentAlertPayloadBuilder {
    override val config: Config = conf
  }

  val now = LocalDate.now
  val thumb = "thumb.jpg"

  val keywordTag = Tag("idKeyword", TagType.Keyword, None, None, "World", "", "")
  val contributorTag = Tag("idContributor", TagType.Contributor, None, None, "Steve", "", "")
  val contributorTag2 = Tag("idContributor2", TagType.Contributor, None, None, "Mike", "", "")
  val seriesTag = Tag("idSeries", TagType.Series, None, None, "Rugby World Cup", "", "")
  val seriesTag2 = Tag("idSeries2", TagType.Series, None, None, "Tetris World Cup", "", "")
  val blogTag = Tag("idBlog", TagType.Blog, None, None, "blogTag", "", "")
  val blogTag2 = Tag("idBlog2", TagType.Blog, None, None, "blogTag2", "", "")

  val contributorTopic = Topic(TagContributor, "idContributor")
  val contributorTopic2 = Topic(TagContributor, "idContributor2")
  val keywordTopic = Topic(TagKeyword, "idKeyword")
  val seriesTopic = Topic(TagSeries, "idSeries")
  val seriesTopic2 = Topic(TagSeries, "idSeries2")
  val blogTopic = Topic(TagBlog, "idBlog")
  val blogTopic1 = Topic(TagBlog, "idBlog1")

  val contentElements = Some(List(
    Element(id = "", relation = "ignore me", `type` = ElementType.Image, assets = Nil),
    Element(id = "", relation = "main", `type` = ElementType.Image, assets = List(
      //Asset(`type` = AssetType.Image, mimeType = None, file = Some("https://some.url/malformedimage.jpg"), typeData = Some(AssetFields(width = Some("malformed int")))),
      Asset(`type` = AssetType.Image, mimeType = None, file = Some("https://some.url/image0.jpg"), typeData = Some(AssetFields(width = Some(0)))),
      Asset(`type` = AssetType.Image, mimeType = None, file = Some("https://some.url/imageNothing.jpg"), typeData = None),
      Asset(`type` = AssetType.Image, mimeType = None, file = Some("https://some.url/image500000.jpg"), typeData = Some(AssetFields(width = Some(500000)))),
      Asset(`type` = AssetType.Image, mimeType = None, file = Some("https://some.url/image1000.jpg"), typeData = Some(AssetFields(width = Some(1000)))),
      Asset(`type` = AssetType.Image, mimeType = None, file = Some("https://some.url/image749.jpg"), typeData = Some(AssetFields(width = Some(749)))),
      Asset(`type` = AssetType.Image, mimeType = None, file = Some("https://some.url/image100.jpg"), typeData = Some(AssetFields(width = Some(100))))
    ))
  ))

  val allTopics = Set(contributorTopic, contributorTopic2, keywordTopic, blogTopic, blogTopic1, seriesTopic, seriesTopic2)
  val link = GuardianLinkDetails("newId", Some("http://gu.com/p/1234"), "webTitle", Some(thumb), GITContent)

  private val contentFields = ContentFields(shortUrl = Some("http://gu.com/p/1234"), thumbnail = Some("thumb.jpg"), headline = Some("headline"))
  val item = Content(
    id = "newId",
    sectionId = None,
    sectionName = None,
    webPublicationDate = Some(CapiDateTime(System.currentTimeMillis(), "Blah")),
    webTitle = "webTitle",
    webUrl = "webUrl",
    apiUrl = "apiUrl",
    fields = Some(contentFields),
    tags = List(keywordTag),
    elements = None,
    references = Nil,
    isExpired = None
  )

  val expectedPayloadForItem = ContentAlertPayload(
    title = "Following: headline",
    message = "webTitle",
    thumbnailUrl = Some(new URI(thumb)),
    sender = "mobile-notifications-content",
    link = link,
    importance = Importance.Major,
    topic = Set(keywordTopic),
    debug = false
  )

  "Content Alert Payload Builder" must {
    "not build payload without tage" in {
      val unTaggedItem = item.copy(tags = Nil)
      builder.buildPayLoad(unTaggedItem) mustEqual None
    }

    "not build payload when no tag can be translated to a topic " in {
      val unTaggedItem = item.copy(tags = List(Tag("blah", TagType.PaidContent, None, None, "random title", "", "")))
      builder.buildPayLoad(unTaggedItem) mustEqual None
    }

    "create content alert for payload" in {
      builder.buildPayLoad(item) mustEqual Some(expectedPayloadForItem)
    }

    "create content alert payload for content with standfirst" in {
      val fieldsWithStandfirst = contentFields.copy(standfirst = Some("<b>some standfirst</b>"))
      val itemWithStandfirst = item.copy(fields = Some(fieldsWithStandfirst))
      val payloadWithStandFirst = expectedPayloadForItem.copy(message = "some standfirst")
      builder.buildPayLoad(itemWithStandfirst) mustEqual Some(payloadWithStandFirst)
    }

    "series tag should take precedence over the rest of the tags if there is only one" in {
      verifyContentAlert(tags = List(contributorTag, seriesTag, keywordTag, blogTag), expectedReason = seriesTag.webTitle)
    }

    "not use series tags in web title if there is more than one" in {
      verifyContentAlert(tags = List(contributorTag, seriesTag, keywordTag, seriesTag2, blogTag), expectedReason = blogTag.webTitle)
    }

    "content tag should take precedence over contributor when generating web title" in {
      verifyContentAlert(tags = List(contributorTag, keywordTag, blogTag), expectedReason = blogTag.webTitle)
    }

    "use contributor for webtitle if there are no other relevant tags" in {
      verifyContentAlert(tags = List(contributorTag, keywordTag), expectedReason = contributorTag.webTitle)
    }

    "not use contributor tags in web title if there is more than one" in {
      verifyContentAlert(tags = List(contributorTag, keywordTag, contributorTag2), expectedReason = "Following")
    }

    "use no imageUri if no image is found" in {
      val itemWithoutImages = item.copy(elements = None)
      val expectedPayloadWithoutImages = expectedPayloadForItem.copy(imageUrl = None)
      builder.buildPayLoad(itemWithoutImages) mustEqual Some(expectedPayloadWithoutImages)
    }

    "use imageUri when a valid main image is found" in {
      val itemWithImages = item.copy(elements = contentElements)
      val expectedPayloadWithImages = expectedPayloadForItem.copy(imageUrl = Some(new URI("https://some.url/image1000.jpg")))
      val load = builder.buildPayLoad(itemWithImages)
      load mustEqual Some(expectedPayloadWithImages)
    }

    "do not prefix title for weekend round up" in {
      val tag = Tag("membership/series/weekend-round-up", TagType.Series, None, None, "Steve", "", "")
      val topic = Topic(TagSeries, "membership/series/weekend-round-up")
      val minuteItem = item.copy(tags = List(tag))
      val expectedMinutePayload = expectedPayloadForItem.copy(title = "headline", topic = Set(topic))
      builder.buildPayLoad(minuteItem) mustEqual Some(expectedMinutePayload)
    }

    "do not prefix title for weekend reading" in {
      val tag = Tag("membership/series/weekend-reading", TagType.Series, None, None, "Steve", "", "")
      val topic = Topic(TagSeries, "membership/series/weekend-reading")
      val minuteItem = item.copy(tags = List(tag))
      val expectedMinutePayload = expectedPayloadForItem.copy(title = "headline", topic = Set(topic))
      builder.buildPayLoad(minuteItem) mustEqual Some(expectedMinutePayload)
    }

    "do not prefix title for guardian-morning-briefing" in {
      val tag = Tag("world/series/guardian-morning-briefing", TagType.Series, None, None, "Steve", "", "")
      val topic = Topic(TagSeries, "world/series/guardian-morning-briefing")
      val contentItem = item.copy(tags = List(tag))
      val expectedPayload = expectedPayloadForItem.copy(title = "headline", topic = Set(topic))
      builder.buildPayLoad(contentItem) mustEqual Some(expectedPayload)
    }

    "use a specific title for the minute notification" in {
      val briefingTag = Tag("us-news/series/the-campaign-minute-2016", TagType.Series, None, None, "Steve", "", "")
      val briefingTopic = Topic(TagSeries, "us-news/series/the-campaign-minute-2016")
      val expectedTitle = "Got a minute? headline"
      val contentItem = item.copy(tags = List(briefingTag))
      val expectedMinutePayload = expectedPayloadForItem.copy(title = expectedTitle, topic = Set(briefingTopic))
      builder.buildPayLoad(contentItem) mustBe Some(expectedMinutePayload)

    }

    "should have a maximum of 20 topics" in {
      val manyTags = (1 to 25).toList.map(index => Tag(s"idKeyword_$index", TagType.Keyword, None, None, s"World_$index", "", ""))
      val contentItem = item.copy(tags = manyTags)
      val expectedTopics = manyTags.map(tag => Topic(TagKeyword, tag.id)).take(20).toSet
      val expectedPayload = expectedPayloadForItem.copy(topic = expectedTopics)

      builder.buildPayLoad(contentItem) mustEqual Some(expectedPayload)
    }
  }

  def verifyContentAlert(tags: List[Tag], expectedReason: String) = {
    val expectedTopics = allTopics.filter { topic => tags.map(_.id).contains(topic.name) }
    val expectedTitle = expectedPayloadForItem.title.replace("Following", expectedReason)
    val expectedPayload = expectedPayloadForItem.copy(title = expectedTitle, topic = expectedTopics)

    val content = item.copy(tags = tags)
    val payLoad = builder.buildPayLoad(content)
    payLoad mustEqual Some(expectedPayload)

  }

}
