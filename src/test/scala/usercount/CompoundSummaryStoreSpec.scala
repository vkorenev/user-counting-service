package usercount
import cats.effect._
import fs2._
import org.specs2.mutable.Specification
import usercount.SummaryH2DB.{Config => DbConfig}

import scala.concurrent.ExecutionContext

class CompoundSummaryStoreSpec extends Specification with H2DBSpec {
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  "not persisted" >> {
    val (
      summaryFor10,
      summaryFor11,
      inMemorySummaries,
      persistedSummariesFor10,
      persistedSummariesFor11,
      lastSavedHour) = (for {
      inMemoryStatsStore <- InMemoryStatsStore[IO]()
      persistentStore <- SummaryH2DB[IO](DbConfig(testDbUrl, "sa", ""))
      summaryStore <- CompoundSummaryStore(inMemoryStatsStore, persistentStore)
      events = Seq(Event(10, "user_1", Impression), Event(11, "user_1", Click))
      _ <- Stream.emits(events).through(summaryStore.eventSink).compile.drain
      summaryFor10 <- summaryStore.hourSummary(10)
      summaryFor11 <- summaryStore.hourSummary(11)
      inMemorySummaries <- inMemoryStatsStore.hourSummaries.compile.toList
      persistedSummariesFor10 <- persistentStore.getSummary(10)
      persistedSummariesFor11 <- persistentStore.getSummary(11)
      lastSavedHour <- persistentStore.getLastHour
    } yield
      (summaryFor10, summaryFor11, inMemorySummaries, persistedSummariesFor10, persistedSummariesFor11, lastSavedHour))
      .unsafeRunSync()

    "compound result 1" >> {
      summaryFor10 must_=== Summary(1, 0, 1)
    }

    "compound result 2" >> {
      summaryFor11 must_=== Summary(0, 1, 1)
    }

    "in-memory store" >> {
      inMemorySummaries must contain(exactly((10L, Summary(1, 0, 1)), (11L, Summary(0, 1, 1))))
    }

    "persisted result 1" >> {
      persistedSummariesFor10 must_=== Summary.empty
    }

    "presisted result 2" >> {
      persistedSummariesFor11 must_=== Summary.empty
    }

    "last saved hour" >> {
      lastSavedHour must beNone
    }
  }

  "persisted" >> {
    val (
      summaryFor10,
      summaryFor11,
      inMemorySummaries,
      persistedSummariesFor10,
      persistedSummariesFor11,
      lastSavedHour) = (for {
      inMemoryStatsStore <- InMemoryStatsStore[IO]()
      persistentStore <- SummaryH2DB[IO](DbConfig(testDbUrl, "sa", ""))
      summaryStore <- CompoundSummaryStore(inMemoryStatsStore, persistentStore)
      events = Seq(Event(10, "user_1", Impression), Event(11, "user_1", Click))
      _ <- Stream.emits(events).through(summaryStore.eventSink).compile.drain
      _ <- summaryStore.saveSummariesToDb(10)
      summaryFor10 <- summaryStore.hourSummary(10)
      summaryFor11 <- summaryStore.hourSummary(11)
      inMemorySummaries <- inMemoryStatsStore.hourSummaries.compile.toList
      persistedSummariesFor10 <- persistentStore.getSummary(10)
      persistedSummariesFor11 <- persistentStore.getSummary(11)
      lastSavedHour <- persistentStore.getLastHour
    } yield
      (summaryFor10, summaryFor11, inMemorySummaries, persistedSummariesFor10, persistedSummariesFor11, lastSavedHour))
      .unsafeRunSync()

    "compound result 1" >> {
      summaryFor10 must_=== Summary(1, 0, 1)
    }

    "compound result 2" >> {
      summaryFor11 must_=== Summary(0, 1, 1)
    }

    "in-memory store" >> {
      inMemorySummaries must contain(exactly((11L, Summary(0, 1, 1))))
    }

    "persisted result 1" >> {
      persistedSummariesFor10 must_=== Summary(1, 0, 1)
    }

    "presisted result 2" >> {
      persistedSummariesFor11 must_=== Summary.empty
    }

    "last saved hour" >> {
      lastSavedHour must beSome(10L)
    }
  }

  "persisted and reopened" >> {
    val dbConfig = DbConfig(testDbUrl, "sa", "")
    val (summaryFor10, persistedSummariesFor10, persistedSummariesFor11, lastSavedHour) = (for {
      inMemoryStatsStore1 <- InMemoryStatsStore[IO]()
      persistentStore1 <- SummaryH2DB[IO](dbConfig)
      summaryStore1 <- CompoundSummaryStore(inMemoryStatsStore1, persistentStore1)
      events = Seq(Event(10, "user_1", Impression), Event(11, "user_1", Click))
      _ <- Stream.emits(events).through(summaryStore1.eventSink).compile.drain
      _ <- summaryStore1.saveSummariesToDb(10)
      inMemoryStatsStore2 <- InMemoryStatsStore[IO]()
      persistentStore2 <- SummaryH2DB[IO](dbConfig)
      summaryStore2 <- CompoundSummaryStore(inMemoryStatsStore2, persistentStore2)
      summaryFor10 <- summaryStore2.hourSummary(10)
      persistedSummariesFor10 <- persistentStore2.getSummary(10)
      persistedSummariesFor11 <- persistentStore2.getSummary(11)
      lastSavedHour <- persistentStore2.getLastHour
    } yield (summaryFor10, persistedSummariesFor10, persistedSummariesFor11, lastSavedHour)).unsafeRunSync()

    "compound result 1" >> {
      summaryFor10 must_=== Summary(1, 0, 1)
    }

    "persisted result 1" >> {
      persistedSummariesFor10 must_=== Summary(1, 0, 1)
    }

    "presisted result 2" >> {
      persistedSummariesFor11 must_=== Summary.empty
    }

    "last saved hour" >> {
      lastSavedHour must beSome(10L)
    }
  }
}
