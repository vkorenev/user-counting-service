package usercount

import java.util.concurrent.TimeUnit

import cats.effect._
import cats.implicits._
import cron4s.Cron
import eu.timepit.fs2cron.awakeEveryCron
import fs2.Stream
import fs2.concurrent.Queue
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._
import usercount.SummaryH2DB.{Config => DbConfig}

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

  def stream[F[_]: ConcurrentEffect: Timer: ContextShift](
      host: String,
      port: Int,
      h2dbConfig: DbConfig): fs2.Stream[F, Unit] = {
    val everyHourAtMinute10 = Cron.unsafeParse("* 10 * ? * *")

    for {
      inMemoryStatsStore <- Stream.eval(InMemoryStatsStore[F]())
      persistentStore <- Stream.eval(SummaryH2DB[F](h2dbConfig))
      compoundStore <- Stream.eval(CompoundSummaryStore(inMemoryStatsStore, persistentStore))
      queue <- Stream.eval(Queue.bounded[F, Event](1000))
      _ <- Stream(
        BlazeServerBuilder[F]
          .bindHttp(port, host)
          .withHttpApp(Router("/" -> new Routes[F](queue.enqueue1, compoundStore.hourSummary).routes).orNotFound)
          .serve
          .drain,
        queue.dequeue.through(compoundStore.eventSink).drain,
        (awakeEveryCron[F](everyHourAtMinute10) >> Stream.eval(Clock[F].realTime(TimeUnit.HOURS))).evalMap { hour =>
          compoundStore.saveSummariesToDb(hour - 1)
        }.drain
      ).parJoin(3)
    } yield ()
  }
}
