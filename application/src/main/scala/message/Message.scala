package message

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.{ActorRef, Behavior}
import utils.Info

import scala.collection.immutable.Queue

trait Message

trait PingServiceMsg extends Message
case class Ping(replyTo: ActorRef[PingServiceMsg]) extends PingServiceMsg
case class Pong(info:Info) extends PingServiceMsg

trait InputServiceMsg extends Message
case class Config(replyTo:ActorRef[Message], args:Queue[String]) extends InputServiceMsg
case class Input(replyTo:ActorRef[Message], arg:String) extends InputServiceMsg
case class InputServiceSuccess(author: Info) extends InputServiceMsg
case class InputServiceFailure(cause: String) extends InputServiceMsg
case class SetOutputRef(ref:ActorRef[OutputServiceMsg]) extends InputServiceMsg

trait OutputServiceMsg extends Message
case class Output(s:String) extends OutputServiceMsg

trait SupervisorServiceMsg extends Message
case class InputsListing(listing: Set[ActorRef[InputServiceMsg]]) extends SupervisorServiceMsg
case class GetInputs() extends SupervisorServiceMsg

case class OutputListing(listing: Set[ActorRef[OutputServiceMsg]]) extends SupervisorServiceMsg
case class GetOutputs() extends SupervisorServiceMsg


