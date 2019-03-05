package usercount

import java.nio.file.Paths

import cats.effect._
import cats.implicits._
import pureconfig.generic.auto._
import pureconfig.module.catseffect._
import usercount.SummaryH2DB.{Config => DbConfig}

object Server extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    for {
      cfg <- loadConfigF[IO, Config](Paths.get("server.conf"))
      server <- ServerStream.stream[IO](cfg.host, cfg.port, cfg.db).compile.drain.as(ExitCode.Success)
    } yield server

  case class Config(host: String, port: Int, db: DbConfig)
}
