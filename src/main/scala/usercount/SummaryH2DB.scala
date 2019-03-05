package usercount

import cats.effect._
import cats.implicits._
import doobie._
import doobie.h2._
import doobie.implicits._

import scala.language.higherKinds

class SummaryH2DB[F[_]: Sync] private (transactor: Resource[F, H2Transactor[F]]) {
  def getSummary(hour: Hour): F[Summary] =
    transactor.use { xa =>
      sql"select impressions, clicks, uniqueUsers from summary where hour=$hour"
        .query[Summary]
        .option
        .map(_.getOrElse(Summary.empty))
        .transact(xa)
    }

  def saveSummary(hour: Hour, summary: Summary): F[Unit] =
    transactor.use { xa =>
      sql"""insert into summary (hour, impressions, clicks, uniqueUsers)
            values ($hour, ${summary.impressions}, ${summary.clicks}, ${summary.uniqueUsers})""".update.run
        .map(_ => ())
        .transact(xa)
    }

  def getLastHour: F[Option[Hour]] =
    transactor.use { xa =>
      sql"select max(hour) from summary".query[Option[Hour]].unique.transact(xa)
    }
}

object SummaryH2DB {
  private[this] def createSummaryTable[F[_]: Sync](transactor: Resource[F, H2Transactor[F]]) =
    transactor.use { xa =>
      sql"""create table if not exists summary (
              hour bigint primary key,
              impressions integer,
              clicks integer,
              uniqueUsers integer)""".update.run.transact(xa)
    }

  def apply[F[_]: Sync: Async: ContextShift](cfg: Config): F[SummaryH2DB[F]] =
    for {
      xa <- Sync[F].delay {
        for {
          ce <- ExecutionContexts.fixedThreadPool[F](8) // connect EC
          te <- ExecutionContexts.cachedThreadPool[F] // transaction EC
          xa <- H2Transactor.newH2Transactor[F](cfg.url, cfg.userName, cfg.password, ce, te)
        } yield xa
      }
      _ <- createSummaryTable(xa)
    } yield new SummaryH2DB[F](xa)

  case class Config(url: String, userName: String, password: String)
}
