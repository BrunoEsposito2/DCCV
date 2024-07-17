package actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import message.{Message, Ping, Pong}
import utils.Info

object ReachableActor:
  def apply(info:Info): ReachableActor = new ReachableActor(info)

private class ReachableActor(val info:Info):

  def getReachableBehavior: PartialFunction[Message, Behavior[Message]] =
     case Ping(replyTo) => replyTo ! Pong(info)
      Behaviors.same
      
  def behavior(): Behavior[Message] = Behaviors.setup { context =>
    Behaviors.receiveMessagePartial(getReachableBehavior)
  }