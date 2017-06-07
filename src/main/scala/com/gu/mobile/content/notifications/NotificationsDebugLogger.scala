package com.gu.mobile.content.notifications

/**
 * Created by nbennett on 05/06/17.
 */
trait NotificationsDebugLogger {

  val showDebug: Boolean = false

  def logDebug(message: String): Unit = {
    if (showDebug) {
      println(message)
    }
  }

  def log(message: String) = println(message)

}
