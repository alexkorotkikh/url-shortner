package com.alexkorotkikh.urlshortner

import com.top10.redis.SingleRedis
import com.twitter.finatra.FinatraServer
import com.twitter.finatra.test.SpecHelper
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, FlatSpec}

class UrlControllerTest extends FlatSpec with SpecHelper with Matchers with MockFactory {

  override def server = {
    val server = new FinatraServer
    server.register(new UrlController with TestRedisSupport)
    server
  }

  "GET /<existing key>" should "redirect to stored URL" in {
    get("/existingKey")

    response.code should equal(301)
    response.getHeader("Location") should equal("http://google.com")
  }

  "GET /<absent key>" should "return status 404" in {
    get("/absentKey")

    response.code should equal(404)
  }

  "POST / with valid URL" should "return generated random key" in {
    post("/", Map("url" -> "http://google.com"))

    response.code should equal(201)
    response.body should fullyMatch regex "[a-zA-Z0-9]{6}"
  }

  "POST / with invalid URL" should "return status 400" in {
    post("/", Map("url" -> "abracadabra"))

    response.code should equal(400)
  }

  "POST / with no URL" should "return status 400" in {
    post("/")

    response.code should equal(400)
  }

  trait TestRedisSupport extends RedisSupport {
    override protected val redis: SingleRedis = stub[TestSingleRedis]

    (redis get _) when "existingKey" returns Some("http://google.com")
    (redis exists _) when "existingKey" returns true

    (redis get _) when * returns None
    (redis exists _) when * returns false

  }

  class TestSingleRedis extends SingleRedis(null)


}

