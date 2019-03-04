package usercount

import cats.effect._
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext

class SummaryH2DBSpec extends Specification with H2DBSpec {
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  "getSummary for empty DB" >> {
    (for {
      db <- SummaryH2DB[IO](testDbUrl, "sa", "")
      summary <- db.getSummary(10)
    } yield summary).unsafeRunSync() must_=== Summary.empty
  }

  "getSummary for non-empty DB" >> {
    (for {
      db <- SummaryH2DB[IO](testDbUrl, "sa", "")
      _ <- db.saveSummary(10, Summary(2, 3, 4))
      summary <- db.getSummary(10)
    } yield summary).unsafeRunSync() must_=== Summary(2, 3, 4)
  }

  "getLastHour for empty DB" in {
    (for {
      db <- SummaryH2DB[IO](testDbUrl, "sa", "")
      lastHour <- db.getLastHour
    } yield lastHour).unsafeRunSync() must beNone
  }

  "getLastHour for non-empty DB" in {
    (for {
      db <- SummaryH2DB[IO](testDbUrl, "sa", "")
      _ <- db.saveSummary(12, Summary(2, 3, 4))
      _ <- db.saveSummary(14, Summary(2, 3, 4))
      lastHour <- db.getLastHour
    } yield lastHour).unsafeRunSync() must beSome(14L)
  }
}
