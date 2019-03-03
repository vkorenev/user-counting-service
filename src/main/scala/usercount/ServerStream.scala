package usercount

import cats.effect._
import cats.implicits._
import fs2.Stream
import fs2.concurrent.Queue
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._

import scala.language.higherKinds

object ServerStream {
  class Routes[F[_]: Sync](addEvent: Event => F[Unit], getHourSummary: Hour => F[Summary]) extends Http4sDsl[F] {

    private[this] object TimestampParamMatcher extends QueryParamDecoderMatcher[Timestamp]("timestamp")

    private[this] object UserParamMatcher extends QueryParamDecoderMatcher[String]("user")

    private[this] object EventParamMatcher extends QueryParamDecoderMatcher[String]("event")

    val routes: HttpRoutes[F] = HttpRoutes.of[F] {
      // POST /analytics?timestamp={millis_since_epoch}&user={user_id}&event={click|impression}
      case POST -> Root / "analytics" :? TimestampParamMatcher(timestamp) +&
            UserParamMatcher(userId) +&
            EventParamMatcher(event) =>
        event match {
          case "click" =>
            addEvent(Event(timestamp.hour, userId, Click)) *> NoContent()
          case "impression" =>
            addEvent(Event(timestamp.hour, userId, Impression)) *> NoContent()
          case _ =>
            BadRequest("Unsupported event type")
        }
      // GET /analytics?timestamp={millis_since_epoch}
      case GET -> Root / "analytics" :? TimestampParamMatcher(timestamp) =>
        val hour = timestamp.hour
        for {
          summary <- getHourSummary(hour)
          response <- Ok(s"""unique_users,${summary.uniqueUsers}
                            |clicks,${summary.clicks}
                            |impressions,${summary.impressions}
                            |""".stripMargin)
        } yield response
    }
  }

  def stream[F[_]: ConcurrentEffect: Timer](host: String, port: Int): fs2.Stream[F, Unit] =
    for {
      inMemoryStatsStore <- Stream.eval(InMemoryStatsStore[F]())
      queue <- Stream.eval(Queue.bounded[F, Event](1000))
      _ <- Stream(
        BlazeServerBuilder[F]
          .bindHttp(port, host)
          .withHttpApp(Router("/" -> new Routes[F](queue.enqueue1, inMemoryStatsStore.hourSummary).routes).orNotFound)
          .serve
          .drain,
        queue.dequeue.through(inMemoryStatsStore.eventSink).drain
      ).parJoin(2)
    } yield ()
}
