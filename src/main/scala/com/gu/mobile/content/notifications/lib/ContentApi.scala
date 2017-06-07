package com.gu.mobile.content.notifications.lib

import com.gu.contentapi.client.model.v1.{ Asset, Content }
import org.joda.time.DateTime
import org.jsoup.Jsoup

object ContentApi {

  implicit class RichContent(content: Content) {

    def standFirst: Option[String] = content.fields.flatMap { f => f.standfirst }

    def textStandFirst: Option[String] = standFirst.map { sf => Jsoup.parse(sf).text() }

    def thumbNail: Option[String] = content.fields.flatMap { f => f.thumbnail }

    def shortUrl: Option[String] = content.fields.flatMap { f => f.shortUrl }

    //TODO - should this return false by default
    def isRecent: Boolean = {
      val anHourAgo = DateTime.now().minusHours(1)
      content.webPublicationDate.map { pubDate =>
        val contentDateTime = new DateTime(pubDate.dateTime)
        contentDateTime.isAfter(anHourAgo)
      }.getOrElse(true)
    }

    def getLoggablePublicationDate: String = {
      content.webPublicationDate.map(_.toString()).getOrElse("Unknown date")
    }
  }

  implicit class RichAsset(asset: Asset) {
    def assetWidth: Option[Int] = asset.typeData.flatMap { td => td.width }
  }

}
