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
  }
}
