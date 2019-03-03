package usercount

import java.time.Instant

import org.specs2.mutable.Specification

class TimestampSpec extends Specification {
  "Timestamp" >> {
    "returns same hour" >> {
      val hourStart = Instant.parse("2000-01-01T10:00:00.000Z").toEpochMilli
      val hourEnd = Instant.parse("2000-01-01T10:59:59.999Z").toEpochMilli
      hourStart.hour must_=== hourEnd.hour
    }
    "returns next hour" >> {
      val hourEnd = Instant.parse("2000-01-01T10:59:59.999Z").toEpochMilli
      val hourStart = Instant.parse("2000-01-01T11:00:00.000Z").toEpochMilli
      hourStart.hour must_=== hourEnd.hour + 1
    }
  }
}
