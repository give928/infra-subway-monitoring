import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class SubwaySimulation extends Simulation {
  val url = "https://give928.kro.kr"
  var password = "1234"
  var age = 30

  val httpMain: HttpRequestBuilder = http("main")
    .get(url)
    .check(status.is(200))

  val httpJoin: HttpRequestBuilder = http("join")
    .post(url + "/members")
    .header("Content-Type", "application/json")
    .check(status.is(201))

  val httpLogin: HttpRequestBuilder = http("login")
    .post(url + "/login/token")
    .header("Content-Type", "application/json")
    .check(status.is(200))
    .check(jsonPath("$..accessToken").saveAs("accessToken"))

  val httpFavorite: HttpRequestBuilder = http("favorite")
    .get(url + "/favorites")
    .check(status.is(200))

  val httpPath: HttpRequestBuilder = http("path")
    .get(url + "/paths")
    .queryParam("source", "113")
    .queryParam("target", "173")
    .check(status.is(200))
    .check(jsonPath("$..distance").ofType[Int])

  val scn: ScenarioBuilder = scenario("main > join > login > favorite > path")
    .exec(httpMain)
    .exec(session => {
      session.set("email", makeEmail())
    })
    .exec(httpJoin
      .body(StringBody(session =>
        s"""{
           |"email": "${session("email").as[String]}",
           |"password": "${password}",
           |"age": ${age}
           |}""".stripMargin)))
    .exec(httpLogin
      .body(StringBody(session =>
        s"""{
           |"email": "${session("email").as[String]}",
           |"password": "${password}"
           |}""".stripMargin)))
    .exec(httpFavorite
      .header("Authorization", session => s"Bearer ${session("accessToken").as[String]}")
    )
    .exec(httpPath)

  // Smoke Test
  setUp(
    scn.inject(atOnceUsers(1))
  )

  // Load Test
  /*setUp(
    scn.inject(
      heavisideUsers(50) during (10 seconds),
      heavisideUsers(100) during (10 seconds),
      heavisideUsers(100) during (10 seconds),
      heavisideUsers(100) during (10 seconds),
      heavisideUsers(50) during (10 seconds)
    )
  )*/

  // Stress Test
  /*setUp(
    scn.inject(
      heavisideUsers(100) during (10 seconds),
      heavisideUsers(200) during (10 seconds),
      heavisideUsers(300) during (10 seconds),
      heavisideUsers(400) during (10 seconds),
      heavisideUsers(100) during (10 seconds)
    )
  )*/

  val makeEmail: () => String = () => UUID.randomUUID().toString.replaceAll("-", "") + "@test.com"
}
