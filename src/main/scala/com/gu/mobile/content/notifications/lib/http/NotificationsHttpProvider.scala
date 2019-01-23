
package com.gu.mobile.content.notifications.lib.http

import com.gu.mobile.notifications.client.{ ContentType, HttpProvider, HttpResponse, _ }
import okhttp3._

import scala.concurrent.{ ExecutionContext, Future }

object NotificationsHttpProvider extends HttpProvider {

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  val client = new OkHttpClient.Builder()
    .retryOnConnectionFailure(true)
    .build()

  def post(postUrl: String, apiKey: String, contentType: ContentType, body: Array[Byte]): Future[HttpResponse] = {
    val mediaType = MediaType.parse(s"${contentType.mediaType}; charset=${contentType.charset}")
    val authHeader = s"Bearer $apiKey"
    val tbody = RequestBody.create(mediaType, body)
    val request = new Request.Builder()
      .url(postUrl)
      .header("Authorization", authHeader)
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
