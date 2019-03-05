package usercount

import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import fs2._

import scala.language.higherKinds

class CompoundSummaryStore[F[_]: Sync] private (
    inMemoryStore: InMemoryStatsStore[F],
    persistentStore: SummaryH2DB[F],
    lastSavedHourRef: Ref[F, Option[Hour]]) {
  def hourSummary(hour: Hour): F[Summary] =
    for {
      lastSavedHour <- lastSavedHourRef.get
      summary <- if (lastSavedHour.exists(hour <= _)) {
        persistentStore.getSummary(hour)
      } else {
        inMemoryStore.hourSummary(hour)
      }
    } yield summary

  def eventSink: Pipe[F, Event, Unit] = inMemoryStore.eventSink

  def saveSummariesToDb(hour: Hour): F[Unit] =
    for {
      lastSavedHour <- lastSavedHourRef.get
      _ <- inMemoryStore.hourSummaries
        .filter {
          case (h, _) => h <= hour && lastSavedHour.forall(_ < h)
        }
        .evalMap((persistentStore.saveSummary _).tupled)
        .compile
        .drain
      _ <- lastSavedHourRef.set(Some(hour))
      _ <- inMemoryStore.removeForHourAndBefore(hour)
    } yield ()
}

object CompoundSummaryStore {
  def apply[F[_]: Sync](
      inMemoryStore: InMemoryStatsStore[F],
      persistentStore: SummaryH2DB[F]): F[CompoundSummaryStore[F]] =
    for {
      lastSavedHour <- persistentStore.getLastHour
      lastSavedHourRef <- Ref.of[F, Option[Hour]](lastSavedHour)
    } yield new CompoundSummaryStore(inMemoryStore, persistentStore, lastSavedHourRef)
}
