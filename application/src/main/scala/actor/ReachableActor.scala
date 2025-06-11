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

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.stream.Materializer
import message.{Message, Ping, Pong}
import utils.Info

trait ReachableActor:

  protected def setActorInfo(info: Info)(implicit ctx: ActorContext[Message]): Info =
    if (info.self == null) info.setSelfRef(ctx.self)
    else info
  
  protected def getReachableBehavior(info: Info)(implicit mat: Materializer, context:ActorContext[Message], behavior:Behavior[Message] = Behaviors.same): PartialFunction[Message, Behavior[Message]] =
    case Ping(replyTo) => replyTo ! Pong(info)
      behavior