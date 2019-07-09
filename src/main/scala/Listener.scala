import java.util.concurrent.CompletionStage

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.events._
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.entities.{Emote, Message, User}
import net.dv8tion.jda.core.hooks._
import net.dv8tion.jda.core.requests.restaction.AuditableRestAction.EmptyRestAction
import net.dv8tion.jda.core.requests.{RequestFuture, RestAction}

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._
import scala.util.Random

case class RollResult(rolls: List[Int], mod: Int, total: Int)

class Listener(jda: ⇒ JDA) extends EventListener {
  private val NAT_20_STRING: String = " (NATURAL 20!)"
  private val NAT_1_STRING: String = " (NATURAL 1)"
  private val NAT_BOTH_STRING: String = " (NAT 1 AND 20!)"
  private val R_DICE = raw"((\d+)\s*d(\d+)(?:\s*([\+-]\s*\d+))?)".r

  @tailrec
  private def rollDie(count: Int, die: Int, mod: Int, rolls: List[Int] = Nil, sum: Int = 0): RollResult =
    if (die == 1) RollResult(List.fill(count)(1), mod, mod + count)
    else count match {
      case 0 ⇒ RollResult(rolls, mod, sum + mod)
      case n ⇒
        val roll = Random.between(1, die + 1)
        rollDie(n - 1, die, mod, roll :: rolls, sum + roll)
    }

  private def isMe(user: User) = user.getIdLong == jda.getSelfUser.getIdLong

  override def onEvent(e: Event): Unit = e match {
    case event: MessageReceivedEvent if !event.getAuthor.isBot ⇒
      R_DICE.findAllIn(event.getMessage.getContentDisplay).map {
        case R_DICE(roll, c, d, m) ⇒ {
          val count = c.toInt
          val die = d.toInt
          val mod = Option(m).map(_.filterNot(_.isWhitespace)).getOrElse("0").toInt

          val result = rollDie(count, die, mod)

          val rollDisplays = result.rolls.map(_.toString).reduce(_ + "+" + _)
          val modDisplay =
            if (result.mod == 0) ""
            else if (result.mod > 0) s"+${result.mod}"
            else result.mod.toString

          val rollResult = s"$rollDisplays$modDisplay=${result.total}"

          val natAlert =
            if (die != 20) ""
            else if (result.rolls.contains(20) && result.rolls.contains(1)) NAT_BOTH_STRING
            else if (result.rolls.contains(20)) NAT_20_STRING
            else if (result.rolls.contains(1)) NAT_1_STRING
            else ""

          Some(s"🎲 $roll ⤇ $rollResult$natAlert")
        }
        case _ ⇒ None
      }.filter(_.nonEmpty)
        .map(_.get)
        .reduceOption(_ + "\n" + _)
        .foreach { assembledMsg ⇒
          val a: RequestFuture[Message] = event.getChannel.sendMessage(assembledMsg).submit()
          a.thenCompose { msg: Message ⇒
            getApplicableReaction(msg)
              .map(_.fold(msg.addReaction, msg.addReaction))
              .getOrElse(new EmptyRestAction[Void](jda))
              .submit()
          }
        }
    case _ ⇒ ()
  }

  def getApplicableReaction(msg: Message): Option[Either[String, Emote]] = msg.getContentRaw match {
    case c if c.contains(NAT_BOTH_STRING) || (c.contains(NAT_20_STRING) && c.contains(NAT_1_STRING)) ⇒
      Some(Left("\uD83E\uDD14"))
    case c if c.contains(NAT_20_STRING) ⇒
      msg.getGuild.getEmotesByName("partyparrot", true)
        .asScala.find(e ⇒ e.isAnimated).map(Right(_))
    case c if c.contains(NAT_1_STRING) ⇒
      msg.getGuild.getEmotesByName("monkaS", true)
        .asScala.headOption.map(Right(_))
    case _ ⇒ None
  }
}