package utils

import akka.actor.typed.ActorRef
import message.Message

case class Info(self:ActorRef[Message] = null, linkedActors: Set[ActorRef[Message]] = Set(), actorType: ActorTypes = ActorTypes.Undefined):
  def setSelfRef(ref: ActorRef[Message]): Info = Info(ref, linkedActors, actorType) 
  def addRef(ref: ActorRef[Message]): Info = Info(self, linkedActors + ref, actorType)
  def resetLinkedActors(): Info = Info(self, Set(), actorType)
  def setActorType(newType: ActorTypes):Info = Info(self, linkedActors, newType)