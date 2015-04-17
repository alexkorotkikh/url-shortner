package com.alexkorotkikh.urlshortner

import java.net.URL

import com.top10.redis.SingleRedis
import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.{Controller, FinatraServer, ResponseBuilder}
import com.twitter.util.{Future, Try}
import org.apache.commons.codec.binary.Base64
import org.jboss.netty.handler.codec.http.HttpMethod

import scala.annotation.tailrec
import scala.util.Random

class UrlController extends Controller {
  self: RedisSupport =>

  get("/:key") { request =>
    stats.timeFuture("get/time") {
      request.routeParams.get("key") flatMap { key => redis.get(key) } match {
        case Some(url) =>
          stats.counter("get/correct").incr()
          redirect(url, "", permanent = true).toFuture
        case None =>
          stats.counter("get/incorrect").incr()
          render.status(404).plain("URL not found").toFuture
      }
    }
  }

  post("/") { request =>
    stats.timeFuture("post/time") {
      request.params.get("url") match {
        case None =>
          stats.counter("post/no_param").incr()
          render.status(400).plain("Parameter 'url' is not found in request").toFuture
        case Some(url) if urlNotValid(url) =>
          stats.counter("post/invalid_param").incr()
          render.status(400).plain("Parameter 'url' is not valid URL").toFuture
        case Some(url) =>
          val newKey = generateUniqueKey
          redis.set(newKey, url)
          stats.counter("post/ok").incr()
          render.status(201).plain(newKey).toFuture
      }
    }
  }

  private def urlNotValid(url: String) = Try(new URL(url)).isThrow

  @tailrec private def generateUniqueKey: String = {
    val key = Random.alphanumeric.take(6).mkString
    if (redis.exists(key)) generateUniqueKey
    else key
  }
}

trait RedisSupport {
  protected val redis = new SingleRedis(
    sys.env.getOrElse("REDIS_HOST", "localhost"),
    sys.env.getOrElse("REDIS_PORT", "6379").toInt,
    sys.env.get("REDIS_PWD")
  )
}

class BasicAuthSupport(login: String, password: String) extends SimpleFilter[FinagleRequest, FinagleResponse] {
  private val expectedHeader = s"Basic ${Base64.encodeBase64String(s"$login:$password".getBytes)}"

  override def apply(request: FinagleRequest, service: Service[FinagleRequest, FinagleResponse]): Future[FinagleResponse] = {
    if (request.method == HttpMethod.GET) service(request)
    else request.headerMap.get("Authorization") match {
      case None => Future(ResponseBuilder(401, "Unauthorized"))
      case Some(auth) if auth != expectedHeader => Future(ResponseBuilder(403, "Forbidden"))
      case Some(_) => service(request)
    }
  }
}

trait ServerConfig extends FinatraServer {
  val login = sys.env.getOrElse("HTTP_LOGIN", "johndoe")
  val password = sys.env.getOrElse("HTTP_PASSWORD", "secret")
  addFilter(new BasicAuthSupport(login, password))

  register(new UrlController with RedisSupport)

  val port = sys.env.getOrElse("PORT", "7070")
  System.setProperty("com.twitter.finatra.config.port", s":$port")
  System.setProperty("com.twitter.finatra.config.adminPort", "")
}

object Server extends ServerConfig