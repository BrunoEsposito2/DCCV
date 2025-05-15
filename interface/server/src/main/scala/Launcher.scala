import actor.GUIBackEnd
import akka.actor.typed.ActorSystem
import message.Message

object Launcher extends App:
  val system = ActorSystem[Message](GUIBackEnd().create(), "surveillance-system")