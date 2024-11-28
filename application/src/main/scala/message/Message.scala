/*
 * Distributed Cluster for Computer Vision
 * Copyright (C) 2024 Andrea Ingargiola, Bruno Esposito
 * andrea.ingargiola@studio.unibo.it
 * bruno.esposito@studio.unibo.it
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package message

import akka.actor.typed.ActorRef
import akka.stream.SinkRef
import akka.util.ByteString
import utils.{ChildStatuses, Info, InputServiceErrors}
import scala.collection.immutable.Queue

trait Message

trait PingServiceMsg extends Message
case class Ping(replyTo: ActorRef[PingServiceMsg]) extends PingServiceMsg
case class Pong(info:Info) extends PingServiceMsg

trait InputServiceMsg extends Message
case class Config(replyTo:ActorRef[Message], args:Queue[String]) extends InputServiceMsg
case class ConfigServiceSuccess(author: Info) extends InputServiceMsg
case class Input(replyTo:ActorRef[Message], arg:String) extends InputServiceMsg
case class InputServiceSuccess(author: Info) extends InputServiceMsg
case class InputServiceFailure(cause: InputServiceErrors) extends InputServiceMsg
case class GetChildStatus(replyTo:ActorRef[Message]) extends InputServiceMsg
case class ChildStatus(info:Info, childStatus: ChildStatuses) extends InputServiceMsg

trait OutputServiceMsg extends Message
case class Output(s: String) extends OutputServiceMsg
case class Subscribe(replyTo:ActorRef[Message], sinkRef: SinkRef[ByteString]) extends OutputServiceMsg
case class SubscribeServiceSuccess(info: Info) extends OutputServiceMsg
case class SubscribeServiceFailure(info: Info, cause: InputServiceErrors) extends OutputServiceMsg
case class SwitchToCamera(cameraRef: ActorRef[Message]) extends OutputServiceMsg
case class Unsubscribe(replyTo: ActorRef[Message]) extends OutputServiceMsg
case class CameraMap(replyTo: ActorRef[Message], map: Map[Info, ChildStatuses]) extends OutputServiceMsg

trait SupervisorServiceMsg extends Message
case class InputsListing(listing: Set[ActorRef[Message]]) extends SupervisorServiceMsg
case class GetInputs() extends SupervisorServiceMsg

case class OutputListing(listing: Set[ActorRef[Message]]) extends SupervisorServiceMsg
case class GetOutputs() extends SupervisorServiceMsg