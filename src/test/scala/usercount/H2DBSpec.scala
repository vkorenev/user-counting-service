package usercount

import java.util.concurrent.atomic.AtomicInteger

trait H2DBSpec {
  def testDbUrl: String = s"jdbc:h2:mem:test_${H2DBSpec.dbIx.incrementAndGet()};DB_CLOSE_DELAY=-1"
}

object H2DBSpec {
  private val dbIx = new AtomicInteger(0)
}
