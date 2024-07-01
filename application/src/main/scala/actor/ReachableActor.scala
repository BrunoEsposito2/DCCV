package actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import message.{Message, Ping, Pong}
import utils.Info

object ReachableActor:
  def apply(info:Info): ReachableActor = new ReachableActor(info)

private class ReachableActor(val info:Info) {

  def behavior(): Behavior[Message] =
    Behaviors.receive { (context, message) =>
     message match {
       case Ping(replyTo) => replyTo ! Pong(info)
         Behaviors.same
     }
    }
}
