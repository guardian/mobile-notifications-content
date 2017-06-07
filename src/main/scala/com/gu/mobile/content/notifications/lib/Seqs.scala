package com.gu.mobile.content.notifications.lib

object Seqs {

  implicit class RichSeq[A](as: Seq[A]) {
    def findOne(f: A => Boolean): Option[A] = as.filter(f) match {
      case one :: Nil => Some(one)
      case _ => None
    }
  }

}
