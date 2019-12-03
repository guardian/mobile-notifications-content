package com.gu.mobile.content.notifications.lib

import java.net.URI

import com.gu.contentapi.client.model.v1._
import com.gu.mobile.content.notifications.Configuration
import com.gu.mobile.content.notifications.model.KeyEvent
import com.gu.mobile.notifications.client.models.TopicTypes.{ TagBlog, TagContributor, TagKeyword, TagSeries }
import com.gu.mobile.notifications.client.models._
import org.joda.time.{ DateTime, LocalDate }
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{ MustMatchers, WordSpecLike }

class ContentAlertPayloadBuilderSpec extends MockitoSugar with WordSpecLike with MustMatchers {

  val conf = mock[Configuration]
  val builder = new ContentAlertPayloadBuilder {
    override val config: Configuration = conf
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

  val allTopics = List(contributorTopic, contributorTopic2, blogTopic, blogTopic1, seriesTopic, seriesTopic2)
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
    tags = List(seriesTag),
    elements = None,
    references = Nil,
    isExpired = None
  )

  val expectedPayloadForItem = ContentAlertPayload(
    title = Some("Following"),
    message = Some("headline"),
    thumbnailUrl = Some(new URI(thumb)),
    sender = "mobile-notifications-content",
    link = link,
    importance = Importance.Major,
    topic = List(seriesTopic),
    debug = false,
    dryRun = None
  )

  val keyEvent = KeyEvent("blockId", Some("blogPostTitle"), "body", Option(DateTime.now()), Option(DateTime.now()))

  val expectedBlogContentAlert = expectedPayloadForItem.copy(
    title = Some("Liveblog update: blogPostTitle"),
    topic = List(Topic(TopicTypes.Content, "newId")),
    message = Some("webTitle"),
    link = link.copy(blockId = Some("blockId"))
  )

  "Content Alert Payload Builder" must {

    "create content alert for payload" in {
      verifyContentAlert(tags = List(seriesTag), expectedReason = seriesTag.webTitle)
    }

    "series tag should take precedence over the rest of the tags if there is only one" in {
      verifyContentAlert(tags = List(contributorTag, blogTag, seriesTag, keywordTag), expectedReason = seriesTag.webTitle)
    }

    "not use series tags in web title if there is more than one" in {
      verifyContentAlert(tags = List(contributorTag, blogTag, seriesTag, keywordTag, seriesTag2), expectedReason = blogTag.webTitle)
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
      val expectedPayloadWithoutImages = expectedPayloadForItem.copy(imageUrl = None, title = Some("Rugby World Cup"))
      builder.buildPayLoad(itemWithoutImages) mustEqual expectedPayloadWithoutImages
    }

    "use imageUri when a valid main image is found" in {
      val itemWithImages = item.copy(elements = contentElements)
      val expectedPayloadWithImages = expectedPayloadForItem.copy(imageUrl = Some(new URI("https://some.url/image1000.jpg")), title = Some("Rugby World Cup"))
      val load = builder.buildPayLoad(itemWithImages)
      load mustEqual expectedPayloadWithImages
    }

    "do not prefix title for guardian-morning-briefing" in {
      val tag = Tag("world/series/guardian-morning-briefing", TagType.Series, None, None, "Steve", "", "")
      val topic = Topic(TagSeries, "world/series/guardian-morning-briefing")
      val contentItem = item.copy(tags = List(tag))
      val expectedPayload = expectedPayloadForItem.copy(title = None, topic = List(topic))
      builder.buildPayLoad(contentItem) mustEqual expectedPayload
    }
    
    "do not prefix title for andrew-sparrows-election-briefing" in {
      val tag = Tag("politics/series/andrew-sparrows-election-briefing", TagType.Series, None, None, "Steve", "", "")
      val topic = Topic(TagSeries, "politics/series/andrew-sparrows-election-briefing")
      val contentItem = item.copy(tags = List(tag))
      val expectedPayload = expectedPayloadForItem.copy(title = None, topic = List(topic))
      builder.buildPayLoad(contentItem) mustEqual expectedPayload
    }
    
    "do not prefix title for guardian-australia-s-morning-mail" in {
      val tag = Tag("australia-news/series/guardian-australia-s-morning-mail", TagType.Series, None, None, "Steve", "", "")
      val topic = Topic(TagSeries, "australia-news/series/guardian-australia-s-morning-mail")
      val contentItem = item.copy(tags = List(tag))
      val expectedPayload = expectedPayloadForItem.copy(title = None, topic = List(topic))
      builder.buildPayLoad(contentItem) mustEqual expectedPayload
    }
    
    "do not prefix title for guardian-us-briefing" in {
      val tag = Tag("us-news/series/guardian-us-briefing", TagType.Series, None, None, "Steve", "", "")
      val topic = Topic(TagSeries, "politics/series/andrew-sparrows-election-briefing")
      val contentItem = item.copy(tags = List(tag))
      val expectedPayload = expectedPayloadForItem.copy(title = None, topic = List(topic))
      builder.buildPayLoad(contentItem) mustEqual expectedPayload
    }

    "should have a maximum of 3 topics" in {
      val manyTags = (1 to 25).toList.map(index => Tag(s"idKeyword_$index", TagType.Contributor, None, None, s"World_$index", "", ""))
      val contentItem = item.copy(tags = manyTags)
      val expectedTopics = manyTags.map(tag => Topic(TagContributor, tag.id)).take(3)
      val expectedPayload = expectedPayloadForItem.copy(title = Some("Following"), message = Some("headline"), topic = expectedTopics)

      builder.buildPayLoad(contentItem) mustEqual expectedPayload
    }
  }

  "create content alert for content & blogPost" in {
    builder.buildPayLoad(item, keyEvent) mustEqual expectedBlogContentAlert
  }

  "create content alert for content & blogPost without title" in {
    val keyEventWithoutTitle = keyEvent.copy(title = None)
    val expectedContentAlertBlogWithoutTitle = expectedBlogContentAlert.copy(title = Some("Liveblog update: webTitle"), message = None)
    builder.buildPayLoad(item, keyEventWithoutTitle) mustEqual expectedContentAlertBlogWithoutTitle
  }

  def verifyContentAlert(tags: List[Tag], expectedReason: String) = {
    val expectedTopics = allTopics.filter { topic => tags.map(_.id).contains(topic.name) }.take(3)
    val expectedPayload = expectedPayloadForItem.copy(title = Some(expectedReason), topic = expectedTopics)

    val content = item.copy(tags = tags)
    val payLoad = builder.buildPayLoad(content)
    payLoad mustEqual expectedPayload

  }

}
