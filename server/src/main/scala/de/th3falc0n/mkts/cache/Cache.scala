package de.th3falc0n.mkts.cache

import com.sun.jdi.InvalidTypeException

import java.time.Instant
import java.time.Duration
import scala.collection.mutable

object Cache {
  private case class Cached[+T](value: T, time: Instant)

  private val cache = mutable.HashMap.empty[String, Cached[Any]]

  def getOrElseUpdate[T](key: String, duration: Duration, value: => T): T = {
    cache.get(key) match {
      case Some(v: Cached[T]) if Duration.between(v.time, Instant.now).compareTo(duration) < 0 =>
        v.value

      case Some(v: Cached[T]) =>
        val newValue = value
        cache -= key
        cache += (key -> Cached(newValue, Instant.now))
        newValue

      case Some(_) =>
        throw new IllegalArgumentException("Cache.getOrElseUpdate: Key was not of type T")

      case None =>
        val newValue = value
        cache += (key -> Cached(newValue, Instant.now))
        newValue
    }
  }

}
