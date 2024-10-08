package com.gu.mobile.content.notifications.lib

import com.gu.mobile.notifications.client.models.Topic.BreakingNewsUk
import com.gu.mobile.notifications.client.models.TopicTypes.{Breaking, TagSeries}
import com.gu.mobile.notifications.client.models.{BreakingNewsPayload, ContentAlertPayload, DefaultGoalType, ExternalLink, GITContent, GITSection, GITTag, GoalType, GuardianLinkDetails, Importance, NotificationPayload, OwnGoalType, PenaltyGoalType, Topic}
import org.specs2.mutable.Specification
import play.api.libs.json.Json

import java.net.URI
import java.util.UUID

/***
 * These tests are a duplicate of these unit tests in the `com.gu.mobile.notifications.client.models`
 * package: https://github.com/guardian/mobile-n10n/blob/000e4f5271558b7a94d0372cb4190b764e3d27ed/api-models/src/test/scala/com/gu/mobile/notifications/client/models/PayloadsSpec.scala
 * These have been added to this repo because we had an incident where the json serialization was broken because
 * there was an issue with build defintion resolving the correct version of play json: https://github.com/guardian/mobile-notifications-content/pull/82
 * This test here is to add an extra check to C.I to catch this potential issue before a release
 */
class PayloadsSpec extends Specification {

  "NotificationPayload" should {
    def verifySerialization(payload: NotificationPayload, expectedJson: String) = Json.toJson(payload) shouldEqual Json.parse(expectedJson)

    "define serializable Breaking News payload" in {
      val payload = BreakingNewsPayload(
        id = UUID.fromString("30aac5f5-34bb-4a88-8b69-97f995a4907b"),
        title = Some("The Guardian"),
        message = Some("Mali hotel attack: UN counts 27 bodies as hostage situation ends"),
        sender = "test",
        imageUrl = Some(new URI("https://mobile.guardianapis.com/img/media/a5fb401022d09b2f624a0cc0484c563fd1b6ad93/0_308_4607_2764/master/4607.jpg/6ad3110822bdb2d1d7e8034bcef5dccf?width=800&height=-&quality=85")),
        thumbnailUrl = Some(new URI("http://media.guim.co.uk/09951387fda453719fe1fee3e5dcea4efa05e4fa/0_181_3596_2160/140.jpg")),
        link = ExternalLink("http://mylink"),
        importance = Importance.Major,
        topic = List(BreakingNewsUk),
        debug = true,
        dryRun = Some(false)
      )
      val expectedJson =
        """
          |{
          |  "id" : "30aac5f5-34bb-4a88-8b69-97f995a4907b",
          |  "title" : "The Guardian",
          |  "type" : "news",
          |  "message" : "Mali hotel attack: UN counts 27 bodies as hostage situation ends",
          |  "thumbnailUrl" : "http://media.guim.co.uk/09951387fda453719fe1fee3e5dcea4efa05e4fa/0_181_3596_2160/140.jpg",
          |  "sender": "test",
          |  "link" : {
          |    "url": "http://mylink"
          |  },
          |  "imageUrl" : "https://mobile.guardianapis.com/img/media/a5fb401022d09b2f624a0cc0484c563fd1b6ad93/0_308_4607_2764/master/4607.jpg/6ad3110822bdb2d1d7e8034bcef5dccf?width=800&height=-&quality=85",
          |  "importance" : "Major",
          |  "topic" : [ {
          |    "type" : "breaking",
          |    "name" : "uk"
          |  } ],
          |  "debug":true,
          |  "dryRun" : false
          |}
        """.stripMargin

      verifySerialization(payload, expectedJson)
    }


    "define serializable Content Alert payload" in {
      val payload = ContentAlertPayload(
        title = Some("Follow"),
        message = Some("Which countries are doing the most to stop dangerous global warming?"),
        thumbnailUrl = Some(new URI("http://media.guim.co.uk/a07334e4ed5d13d3ecf4c1ac21145f7f4a099f18/127_0_3372_2023/140.jpg")),
        sender = "test",
        link = GuardianLinkDetails(
          contentApiId = "environment/ng-interactive/2015/oct/16/which-countries-are-doing-the-most-to-stop-dangerous-global-warming",
          shortUrl = Some("http:short.com"),
          title = "linkTitle",
          thumbnail = Some("http://thumb.om"),
          git = GITContent),
        importance = Importance.Minor,
        topic = List(Topic(TagSeries, "environment/series/keep-it-in-the-ground"), Topic(Breaking, "n2")),
        debug = false,
        dryRun = Some(false))
      val expectedJson =
        """
          |{
          |  "id" : "7c555802-2658-3656-9fda-b4f044a241cc",
          |  "title" : "Follow",
          |  "type" : "content",
          |  "message" : "Which countries are doing the most to stop dangerous global warming?",
          |  "thumbnailUrl" : "http://media.guim.co.uk/a07334e4ed5d13d3ecf4c1ac21145f7f4a099f18/127_0_3372_2023/140.jpg",
          |  "sender" : "test",
          |  "link" : {
          |    "contentApiId" : "environment/ng-interactive/2015/oct/16/which-countries-are-doing-the-most-to-stop-dangerous-global-warming",
          |    "shortUrl":"http:short.com",
          |    "title":"linkTitle",
          |    "thumbnail":"http://thumb.om",
          |    "git":{"mobileAggregatorPrefix":"item-trimmed"}
          |  },
          |  "importance" : "Minor",
          |  "topic" : [ {
          |    "type" : "tag-series",
          |    "name" : "environment/series/keep-it-in-the-ground"
          |  },{
          |    "type" : "breaking",
          |    "name" : "n2"
          |    }],
          |    "debug" : false,
          |    "dryRun" : false
          |}
        """.stripMargin
      verifySerialization(payload, expectedJson)
    }
  }
}

