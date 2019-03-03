package usercount

import cats.effect.concurrent.Deferred
import cats.effect.{ContextShift, IO}
import org.http4s._
import org.http4s.implicits._
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext

class ServerStreamSpec extends Specification {
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  "POST" >> {
    def post(uri: Uri, addEvent: Event => IO[Unit]): IO[Response[IO]] = {
      val request = Request[IO](Method.POST, uri)
      new ServerStream.Routes[IO](addEvent, _ => IO.never).routes.orNotFound(request)
    }

    def postAndGetEvent(uri: Uri): IO[Event] =
      for {
        eventSink <- Deferred[IO, Event]
        _ <- post(uri, eventSink.complete)
        event <- eventSink.get
      } yield event

    "click" >> {
      val uri = Uri.uri("/analytics?timestamp=12345678&user=user_123&event=click")

      "responds with 204" >> {
        post(uri, _ => IO.unit).unsafeRunSync().status must_=== Status.NoContent
      }

      "returns event" >> {
        postAndGetEvent(uri).unsafeRunSync() must_=== Event(3, "user_123", Click)
      }
    }

    "impression" >> {
      val uri = Uri.uri("/analytics?timestamp=12345678&user=user_123&event=impression")

      "responds with 204" >> {
        post(uri, _ => IO.unit).unsafeRunSync().status must_=== Status.NoContent
      }

      "returns event" >> {
        postAndGetEvent(uri).unsafeRunSync() must_=== Event(3, "user_123", Impression)
      }
    }
  }

  "GET" >> {
    val uri = Uri.uri("/analytics?timestamp=12345678")

    def get(getHourSummary: Hour => IO[Summary]): IO[Response[IO]] = {
      val request = Request[IO](Method.GET, uri)
      new ServerStream.Routes[IO](_ => IO.never, getHourSummary).routes.orNotFound(request)
    }

    "responds with 200" >> {
      get(_ => IO(Summary(30, 20, 10))).unsafeRunSync().status must_=== Status.Ok
    }

    "body" >> {
      get(i => IO(Summary(30, 20, i.toInt))).unsafeRunSync().as[String].unsafeRunSync() must_===
        """unique_users,3
          |clicks,20
          |impressions,30
          |""".stripMargin
    }
  }
}
