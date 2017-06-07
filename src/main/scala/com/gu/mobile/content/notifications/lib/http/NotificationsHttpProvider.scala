package com.gu.mobile.content.notifications.lib.http

import java.util

import com.gu.mobile.notifications.client.{ ContentType, HttpProvider, HttpResponse }

import scala.concurrent.{ ExecutionContext, Future }
import com.gu.mobile.notifications.client._
import com.ning.http.client.Response
import dispatch._

object NotificationsHttpProvider extends HttpProvider {

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  override def post(postUrl: String, contentType: ContentType, body: Array[Byte]): Future[HttpResponse] = {
    println(s"++++++++++++++++++++ URL: $postUrl  ${contentType.toString}")
    def myPost = url(postUrl).POST.setContentType(contentType.mediaType, contentType.charset).setBody(body)
    processResponse(Http(myPost))
  }

  override def get(getUrl: String): Future[HttpResponse] = {
    val resp = Http(url(getUrl))
    processResponse(resp)
  }

  def processResponse(response: Future[Response]) = response map {
    r =>
      val status = r.getStatusCode
      val body = r.getResponseBody
      if (status >= 200 && status < 300) HttpOk(status, body) else HttpError(status, body)

  }

}
