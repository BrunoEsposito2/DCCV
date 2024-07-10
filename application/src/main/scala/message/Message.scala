package message

import akka.actor.typed.{ActorRef, Behavior}
import utils.Info

trait Message

trait PingServiceMsg extends Message
case class Ping(replyTo: ActorRef[PingServiceMsg]) extends PingServiceMsg
case class Pong(info:Info) extends PingServiceMsg

trait InputServiceMsg extends Message
case class ConfigMsg(args:List[String]) extends InputServiceMsg


