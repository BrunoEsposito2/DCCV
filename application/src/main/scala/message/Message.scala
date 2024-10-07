package message

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.SourceRef
import akka.util.ByteString
import utils.{Info, InputServiceErrors}

import scala.collection.immutable.Queue

trait Message

trait PingServiceMsg extends Message
case class Ping(replyTo: ActorRef[PingServiceMsg]) extends PingServiceMsg
case class Pong(info:Info) extends PingServiceMsg

trait InputServiceMsg extends Message
case class Config(replyTo:ActorRef[Message], args:Queue[String]) extends InputServiceMsg
case class ConfigServiceSuccess(author: Info, sourceRef: SourceRef[ByteString]) extends InputServiceMsg
case class Input(replyTo:ActorRef[Message], arg:String) extends InputServiceMsg
case class InputServiceSuccess(author: Info) extends InputServiceMsg
case class InputServiceFailure(cause: InputServiceErrors) extends InputServiceMsg
case class GetSourceRef(replyTo:ActorRef[Message]) extends InputServiceMsg
case class CameraOutputStreamSource(info:Info, sourceRef: SourceRef[ByteString]) extends InputServiceMsg

trait OutputServiceMsg extends Message
case class Output(s:String) extends OutputServiceMsg

trait SupervisorServiceMsg extends Message
case class InputsListing(listing: Set[ActorRef[InputServiceMsg]]) extends SupervisorServiceMsg
case class GetInputs() extends SupervisorServiceMsg

case class OutputListing(listing: Set[ActorRef[OutputServiceMsg]]) extends SupervisorServiceMsg
case class GetOutputs() extends SupervisorServiceMsg


