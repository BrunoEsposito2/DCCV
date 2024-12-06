package util

import akka.actor.typed.ActorRef
import message.Message

trait InterfaceMessage extends Message
case class ForwardConfigData(cameraRef: ActorRef[Message], data: Map[String, Double]) extends InterfaceMessage
