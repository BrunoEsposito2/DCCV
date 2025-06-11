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

package actor

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.stream.javadsl.StreamRefs
import akka.stream.scaladsl.Sink
import akka.stream.Materializer
import akka.util.ByteString
import message.{CameraMap, Config, ConfigServiceSuccess, InputServiceSuccess, Message, OutputServiceMsg, Subscribe, SubscribeServiceFailure, SubscribeServiceSuccess, SwitchToCamera, Unsubscribe}
import utils.ActorTypes.Client
import utils.Info
import scala.collection.immutable.Queue

private type Ref = ActorRef[Message]

/**
 * Message received by a GenericClient to change the consumer function of its sink.
 * @param function is the ByteString => Unit side effect function triggered each time a CameraManager's source emits an output.
 */
case class ConfigureClientSink(function: ByteString => Unit) extends OutputServiceMsg

object GenericClient:
  def apply(client: GenericClient): Behavior[Message] =
    client.create()

  def configureCamera(cameraRef: Ref, selfRef: Ref, args: Queue[String]): Unit =
    cameraRef ! Config(selfRef, args)

trait GenericClient extends ReachableActor:
  def create(): Behavior[Message] =
    Behaviors.setup {
      ctx =>
        ctx.system.receptionist.tell(Receptionist.register(ServiceKey[Message]("outputs"), ctx.self))
        implicit val context: ActorContext[Message] = ctx
        implicit val mat: Materializer = Materializer(context.system)
        this.behavior(super.setActorInfo(Info()).setActorType(Client), Sink.foreach[ByteString](this.startingSinkFunction()))
    }
  
  protected def startingSinkFunction(): ByteString => Unit =
    (bs:ByteString) => bs

  private def getClientBehavior(info:Info, sink:Sink[ByteString, _])(implicit mat: Materializer, context: ActorContext[Message]): PartialFunction[Message, Behavior[Message]] =
    case CameraMap(replyTo, map) =>
      this.onMessage(CameraMap(replyTo, map), info)
      Behaviors.same
      
    case ConfigureClientSink(function) =>
      if(info.linkedActors.nonEmpty) info.self ! SwitchToCamera(info.linkedActors.head.ref)
      this.behavior(info, Sink.foreach(function))
      
    case SwitchToCamera(cameraRef) =>
      this.onMessage(SwitchToCamera(cameraRef), info)
      if (info.linkedActors.nonEmpty && cameraRef != info.linkedActors.head) info.linkedActors.head ! Unsubscribe(context.self)
      cameraRef ! Subscribe(info.self, sink.runWith(StreamRefs.sinkRef()))
      this.behavior(info.resetLinkedActors(), sink)
      
    case SubscribeServiceSuccess(cameraInfo) =>
      this.onMessage(SubscribeServiceSuccess(cameraInfo), info)
      this.behavior(info.resetLinkedActors().addRef(cameraInfo.self), sink)

    case SubscribeServiceFailure(info, cause) =>
      this.onMessage(SubscribeServiceFailure(info, cause), info)
      Behaviors.same
      
    case InputServiceSuccess(cameraInfo) =>
      this.onMessage(InputServiceSuccess(cameraInfo), info)
      this.behavior(info, sink)

    case ConfigServiceSuccess(author) =>
      this.onMessage(ConfigServiceSuccess(author), info)
      this.behavior(info, sink)

    case customMessage: Message =>
      this.onMessage(customMessage, info)
      Behaviors.same

  def onMessage(msg: Message, clientInfo: Info): Unit

  private def behavior(info:Info, sink:Sink[ByteString, _])(implicit mat: Materializer, context: ActorContext[Message]): Behavior[Message] =
    Behaviors.setup { ctx =>
      Behaviors.receiveMessagePartial(getReachableBehavior(info).orElse(getClientBehavior(info, sink)))
    }

