package actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.stream.Materializer
import message.{Message, Ping, Pong}
import utils.Info

trait ReachableActor:

  protected def setActorInfo(info: Info)(implicit ctx: ActorContext[Message]): Info =
    if (info.self == null) info.setSelfRef(ctx.self)
    else info
  
  protected def getReachableBehavior(info: Info)(implicit mat: Materializer, context:ActorContext[Message], behavior:Behavior[Message] = Behaviors.same): PartialFunction[Message, Behavior[Message]] =
    case Ping(replyTo) => replyTo ! Pong(info)
      behavior