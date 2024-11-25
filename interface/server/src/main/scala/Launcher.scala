import actor.Server
import akka.actor.typed.ActorSystem
import message.Message

object Launcher extends App:
  val system = ActorSystem[Message](Server().create(), "surveillance-system")

