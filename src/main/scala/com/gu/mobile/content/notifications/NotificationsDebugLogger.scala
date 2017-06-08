package com.gu.mobile.content.notifications

trait NotificationsDebugLogger {

  val showDebug: Boolean = false

  def logDebug(message: String): Unit = {
    if (showDebug) {
      println(message)
    }
  }

  def log(message: String) = println(message)
}
