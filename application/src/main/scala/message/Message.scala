package message

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.{ActorRef, Behavior}
import utils.Info

import scala.collection.immutable.Queue

trait Message

trait PingServiceMsg extends Message
case class Ping(replyTo: ActorRef[PingServiceMsg]) extends PingServiceMsg
case class Pong(info:Info) extends PingServiceMsg

trait InputServiceMsg extends PingServiceMsg
case class PidMsg(pid: Integer) extends InputServiceMsg
case class ConfigMsg(args:Queue[String]) extends InputServiceMsg
case class InputMsg(arg:String) extends InputServiceMsg
case class SetOutputRef(ref:ActorRef[OutputServiceMsg]) extends InputServiceMsg

trait OutputServiceMsg extends PingServiceMsg
case class Output(s:String) extends OutputServiceMsg

trait SupervisorServiceMsg extends Message
case class InputsListing(listing: Set[ActorRef[InputServiceMsg]]) extends SupervisorServiceMsg
case class GetInputs() extends SupervisorServiceMsg

case class OutputListing(listing: Set[ActorRef[OutputServiceMsg]]) extends SupervisorServiceMsg
case class GetOutputs() extends SupervisorServiceMsg


