package actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.stream.Materializer
import message.{Message, Ping, Pong}
import utils.Info

object ReachableActor:
  def apply(info: Info): ReachableActor = new ReachableActor(info)

private class ReachableActor(val info:Info):

  def create(): Behavior[Message] =
    Behaviors.setup { context =>
      implicit val ctx: ActorContext[Message] = context
      implicit val mat: Materializer = Materializer(context.system)
      ReachableActor(this.setActorInfo(ctx)).behavior
    }

  protected def setActorInfo(ctx: ActorContext[Message]): Info =
    if (info.self == null) info.setSelfRef(ctx.self)
    else info

  protected def behavior(implicit mat: Materializer, context:ActorContext[Message]): Behavior[Message] =
    Behaviors.setup { context =>
      Behaviors.receiveMessagePartial(getReachableBehavior)
    }
  
  protected def getReachableBehavior(implicit mat: Materializer, context:ActorContext[Message]): PartialFunction[Message, Behavior[Message]] =
    case Ping(replyTo) => replyTo ! Pong(info)
      ReachableActor(info).behavior