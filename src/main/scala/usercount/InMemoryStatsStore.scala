package usercount

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import fs2._
import usercount.InMemoryStatsStore.Stats

import scala.language.higherKinds

class InMemoryStatsStore[F[_]: Sync] private (mapRef: Ref[F, Map[Hour, Stats]]) {
  def hourSummary(hour: Hour): F[Summary] = mapRef.get.map { hourStats =>
    hourStats.getOrElse(hour, Stats.empty).summary
  }

  def hourSummaries: Stream[F, (Hour, Summary)] =
    for {
      hourStats <- Stream.eval(mapRef.get)
      summaries <- Stream.emits(hourStats.mapValues(_.summary).toSeq)
    } yield summaries

  def eventSink: Pipe[F, Event, Unit] = _.evalMap { event =>
    val hour = event.hour
    mapRef.update { map =>
      val stats = map.getOrElse(hour, Stats.empty)
      map + (hour -> stats.add(event))
    }
  }

  def removeForHourAndBefore(hour: Hour): F[Unit] = mapRef.update(_.filterKeys(_ > hour))
}

object InMemoryStatsStore {
  def apply[F[_]: Sync](): F[InMemoryStatsStore[F]] =
    Ref.of[F, Map[Hour, Stats]](Map.empty).map(new InMemoryStatsStore[F](_))

  private case class Stats(impressions: Int, clicks: Int, users: Set[String]) {
    def add(event: Event): Stats = event.eventType match {
      case Click      => copy(clicks = clicks + 1, users = users + event.userId)
      case Impression => copy(impressions = impressions + 1, users = users + event.userId)
    }

    def summary: Summary = Summary(impressions, clicks, users.size)
  }

  private object Stats {
    val empty = Stats(0, 0, Set.empty)
  }
}
