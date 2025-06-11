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

/**
 * Trait that support the messages that diffuse the actors info.
 */
trait PingServiceMsg extends Message

/**
 * Request an actor info.
 * @param replyTo is the signature of the actor that is asking the information.
 */
case class Ping(replyTo: ActorRef[PingServiceMsg]) extends PingServiceMsg

/**
 * Send the actor info
 * @param info is the info field of the ReachableActor that contains its signature, its type and its liked actors.
 */
case class Pong(info:Info) extends PingServiceMsg

/**
 * This trait is extended by all the messages used to forward an input to the CV process.
 */
trait InputServiceMsg extends Message

/**
 * Command an actor to configure the CV process with custom logic.
 * @param replyTo is the signature of the actor performing the request.
 * @param args is the string array to translate in a console command.
 */
case class Config(replyTo:ActorRef[Message], args:Queue[String]) extends InputServiceMsg

/**
 * Signal to the requester that the CV process configuration happened succesfully.
 * @param author is the actor that have performed the configuration.
 */
case class ConfigServiceSuccess(author: Info) extends InputServiceMsg

/**
 * Command an actor to forward a string to the CV process.
 * @param replyTo is the signature of the requester.
 * @param arg is the string to forward to the CV process.
 */
case class Input(replyTo:ActorRef[Message], arg:String) extends InputServiceMsg

/**
 * Signal to the requester that the string has been forwarded successfully.
 * @param author is the signature of the actor that has forwarded the string.
 */
case class InputServiceSuccess(author: Info) extends InputServiceMsg

/**
 * Signal to the requester that the string has not been forwarded.
 * @param cause is the error caused by the try.
 */
case class InputServiceFailure(cause: InputServiceErrors) extends InputServiceMsg

/**
 * Ask a CameraManager the status of the CV process.
 * @param replyTo is the signature of the actor that is requesting the information.
 */
case class GetChildStatus(replyTo:ActorRef[Message]) extends InputServiceMsg

/**
 * Send the CV process status.
 * @param info is the info field of the CameraManager that is answering.
 * @param childStatus is the status (IDLE or RUNNING) of the CV process, where IDLE means that no process is actually running.
 */
case class ChildStatus(info:Info, childStatus: ChildStatuses) extends InputServiceMsg

/**
 * This trait is extended by all messages that regulate the propagation of the output of the CV process.
 */
trait OutputServiceMsg extends Message

/**
 * Forward a string as an output.
 * @param s the string that is being sent.
 */
case class Output(s: String) extends OutputServiceMsg

/**
 * Message used by a client to subscribe to a CameraManager (and its CV process) output.
 * @param replyTo is the signature of the client that is trying to subscribe.
 * @param sinkRef is the signature of the client consumer that must be attached to the CameraManager Source.
 */
case class Subscribe(replyTo:ActorRef[Message], sinkRef: SinkRef[ByteString]) extends OutputServiceMsg

/**
 * Signal to the client that the subscription happened succesfully.
 * @param info is the info field of the CameraManager that performed the subscription.
 */
case class SubscribeServiceSuccess(info: Info) extends OutputServiceMsg

/**
 * Signal to the client that the subscription is rejected.
 * @param info is the info field of the CameraManager that tried to perform the subscription.
 * @param cause is the error code that caused the rejection.
 */
case class SubscribeServiceFailure(info: Info, cause: InputServiceErrors) extends OutputServiceMsg

/**
 * Command a client to subscribe to a CameraManager.
 * @param cameraRef is the signature of the CameraManager to which the client must try to subscribe.
 */
case class SwitchToCamera(cameraRef: ActorRef[Message]) extends OutputServiceMsg

/**
 * Disconnect a client's sink from the CameraManager source, stopping the forwarding of its CV process' output to it.
 * @param replyTo is the signature og the client to disconnect.
 */
case class Unsubscribe(replyTo: ActorRef[Message]) extends OutputServiceMsg

/**
 * Get the signatures and child statuses from all currently deployed CameraManagers.
 * @param replyTo is the actor performing the request.
 * @param map is the map off all currently deployed CameraManagers' info field linked with their CV process statuses.
 */
case class CameraMap(replyTo: ActorRef[Message], map: Map[Info, ChildStatuses]) extends OutputServiceMsg

/**
 * This trait will be extended by all messages used by the Supervisor to perform its global view service.
 */
trait SupervisorServiceMsg extends Message

/**
 * Adapter pattern to receive from the Akka Receptionist the list of all the currently deployed actors subscribed to
 * the "inputs" key (usually the CameraManagers).
 * @param listing is the list of all the currently deployed actors subscribed to the "inputs" key.
 */
case class InputsListing(listing: Set[ActorRef[Message]]) extends SupervisorServiceMsg

/**
 * Internal temporized message used by the Supervisor to trigger the request to the Akka Receptionist
 * of all the currently deployed actors subscribed to the "inputs" key
 */
case class GetInputs() extends SupervisorServiceMsg

/**
 * Adapter pattern message to receive from the Akka Receptionist the list of all the currently deployed actors subscribed to
 * the "outputs" key (usually the clients).
 *
 * @param listing is the list of all he currently deployed actors subscribed to the "outputs" key.
 */
case class OutputListing(listing: Set[ActorRef[Message]]) extends SupervisorServiceMsg

/**
 * Internal temporized message used by the Supervisor to trigger the request to the Akka Receptionist
 * of all the currently deployed actors subscribed to the "outputs" key
 */
case class GetOutputs() extends SupervisorServiceMsg