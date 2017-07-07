
package com.gu.mobile.content.notifications.lib.http

import okhttp3.{MediaType, OkHttpClient, Request, RequestBody, Response}
import com.gu.mobile.notifications.client.{ ContentType, HttpProvider, HttpResponse, _ }
import com.ning.http.client.Response

import scala.concurrent.{ ExecutionContext, Future }

object NotificationsHttpProvider extends HttpProvider {

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  val client = new OkHttpClient.Builder()
    .retryOnConnectionFailure(true)
    .build()

  def post(postUrl: String, contentType: ContentType, body: Array[Byte]): Future[HttpResponse] = {
    val mediaType = MediaType.parse(s"${contentType.mediaType}; charset=${contentType.charset}")
    val tbody = RequestBody.create(mediaType, body)
    val request = new Request.Builder()
      .url(postUrl)
      .post(tbody)
      .build()

    val response = client.newCall(request).execute()
    processResponse(
      Future.successful(response)
    )
  }

  def get(getUrl: String): Future[HttpResponse] = {
    val request = new Request.Builder()
      .url(getUrl)
      .build()

    val response = client.newCall(request).execute()
    processResponse(
      Future.successful(response)
    )
  }

  def processResponse(response: Future[Response]) = response map {
    r =>
      val status = r.code
      val body = r.body.string()

      if (status >= 200 && status < 300) HttpOk(status, body) else HttpError(status, body)
  }

}
