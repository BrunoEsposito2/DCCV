package actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import message.{Message, Ping, Pong}
import utils.Info

object ReachableActor:
  def apply(info: Info): ReachableActor = new ReachableActor(info)

private class ReachableActor(val info:Info):

  def create(): Behavior[Message] =
    Behaviors.setup { context =>
      ReachableActor(this.setActorInfo(context)).behavior()
    }

  protected def setActorInfo(ctx: ActorContext[Message]): Info =
    if (info.self == null) info.setSelfRef(ctx.self)
    else info

  protected def behavior(): Behavior[Message] = 
    Behaviors.setup { context =>
      Behaviors.receiveMessagePartial(getReachableBehavior)
    }
  
  protected def getReachableBehavior: PartialFunction[Message, Behavior[Message]] =
    case Ping(replyTo) => replyTo ! Pong(info)
      Behaviors.same