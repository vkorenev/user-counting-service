package usercount

import cats.effect._
import cats.implicits._

object Server extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    ServerStream.stream[IO]("0.0.0.0", 8080).compile.drain.as(ExitCode.Success)
}
