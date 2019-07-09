import net.dv8tion.jda.core._

import scala.jdk.CollectionConverters._

object Main extends App {
  val token = System.getenv.asScala.get("API_BOT_TOKEN")
  require(token.nonEmpty)
  val jda: JDA = new JDABuilder(AccountType.BOT).setToken(token.get).build()
  jda.awaitReady()
  jda.addEventListener(new Listener(jda))
}
