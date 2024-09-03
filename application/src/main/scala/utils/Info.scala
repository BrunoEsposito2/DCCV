package utils

import akka.actor.typed.ActorRef
import message.Message

case class Info(self:ActorRef[_ <: Message] = null, linkedActors: Set[ActorRef[_ <: Message]] = Set(), actorType: String = ""):
  def setSelfRef(ref: ActorRef[_ <: Message]): Info = Info(ref, linkedActors, actorType) 
  def addRef(ref: ActorRef[_ <: Message]): Info = Info(self, linkedActors + ref, actorType)
  def resetLinkedActors(): Info = Info(self, Set(), actorType)
  def setActorType(newType: String):Info = Info(self, linkedActors, newType)

