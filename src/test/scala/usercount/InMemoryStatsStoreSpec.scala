package usercount

import cats.effect._
import fs2._
import org.specs2.mutable.Specification

class InMemoryStatsStoreSpec extends Specification {
  "StatsStore" >> {
    "no events have been added" >> {
      (for {
        statsStore <- InMemoryStatsStore[IO]()
        summary <- statsStore.hourSummary(10)
      } yield summary).unsafeRunSync() must_=== Summary(0, 0, 0)
    }

    "click event has been added" >> {
      (for {
        statsStore <- InMemoryStatsStore[IO]()
        _ <- Stream.emit(Event(10, "user_1", Click)).through(statsStore.eventSink).compile.drain
        summary <- statsStore.hourSummary(10)
      } yield summary).unsafeRunSync() must_=== Summary(0, 1, 1)
    }

    "impression event has been added" >> {
      (for {
        statsStore <- InMemoryStatsStore[IO]()
        _ <- Stream.emit(Event(10, "user_1", Impression)).through(statsStore.eventSink).compile.drain
        summary <- statsStore.hourSummary(10)
      } yield summary).unsafeRunSync() must_=== Summary(1, 0, 1)
    }

    "2 click events for the same user have been added" >> {
      (for {
        statsStore <- InMemoryStatsStore[IO]()
        events = Seq(Event(10, "user_1", Click), Event(10, "user_1", Click))
        _ <- Stream.emits(events).through(statsStore.eventSink).compile.drain
        summary <- statsStore.hourSummary(10)
      } yield summary).unsafeRunSync() must_=== Summary(0, 2, 1)
    }

    "2 click events for different users have been added" >> {
      (for {
        statsStore <- InMemoryStatsStore[IO]()
        events = Seq(Event(10, "user_1", Click), Event(10, "user_2", Click))
        _ <- Stream.emits(events).through(statsStore.eventSink).compile.drain
        summary <- statsStore.hourSummary(10)
      } yield summary).unsafeRunSync() must_=== Summary(0, 2, 2)
    }

    "2 click events for different hours have been added" >> {
      "summary for hour" >> {
        (for {
          statsStore <- InMemoryStatsStore[IO]()
          events = Seq(Event(10, "user_1", Click), Event(12, "user_2", Impression))
          _ <- Stream.emits(events).through(statsStore.eventSink).compile.drain
          summary <- statsStore.hourSummary(10)
        } yield summary).unsafeRunSync() must_=== Summary(0, 1, 1)
      }

      "summaries for all hours" >> {
        (for {
          statsStore <- InMemoryStatsStore[IO]()
          events = Seq(Event(10, "user_1", Click), Event(12, "user_2", Impression))
          _ <- Stream.emits(events).through(statsStore.eventSink).compile.drain
          summaries <- statsStore.hourSummaries.compile.toList
        } yield summaries).unsafeRunSync() must contain(exactly((10L, Summary(0, 1, 1)), (12L, Summary(1, 0, 1))))
      }
    }

    "remove stats" >> {
      (for {
        statsStore <- InMemoryStatsStore[IO]()
        events = Seq(
          Event(10, "user_1", Click),
          Event(11, "user_2", Impression),
          Event(12, "user_1", Click),
          Event(13, "user_2", Impression))
        _ <- Stream.emits(events).through(statsStore.eventSink).compile.drain
        _ <- statsStore.removeForHourAndBefore(11)
        summaries <- statsStore.hourSummaries.compile.toList
      } yield summaries).unsafeRunSync() must contain(exactly((12L, Summary(0, 1, 1)), (13L, Summary(1, 0, 1))))
    }
  }
}
