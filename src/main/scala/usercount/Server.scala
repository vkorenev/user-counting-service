package usercount

import cats.effect._
import cats.implicits._
import usercount.SummaryH2DB.{Config => DbConfig}

object Server extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    val dbConfig = DbConfig("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
    ServerStream.stream[IO]("0.0.0.0", 8080, dbConfig).compile.drain.as(ExitCode.Success)
  }
}
