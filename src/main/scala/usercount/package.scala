package object usercount {
  type Timestamp = Long
  type Hour = Long

  implicit class TimestampSyntax(val timestamp: Timestamp) extends AnyVal {
    def hour: Hour = timestamp / 3600000
  }

  sealed trait EventType
  case object Click extends EventType
  case object Impression extends EventType

  case class Event(hour: Hour, userId: String, eventType: EventType)

  case class Summary(impressions: Int, clicks: Int, uniqueUsers: Int)

  object Summary {
    val empty = Summary(0, 0, 0)
  }
}
