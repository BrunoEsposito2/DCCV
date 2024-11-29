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

import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import message.{CameraMap, ChildStatus, GetChildStatus, GetInputs, GetOutputs, InputServiceMsg, InputsListing, Message, OutputListing, Pong}
import utils.{ChildStatuses, Info}

import scala.collection.immutable.HashMap
import scala.concurrent.duration.{FiniteDuration, SECONDS}


object Supervisor:
  def apply(): Supervisor = new Supervisor()

class Supervisor:
  def create(): Behavior =
    Behaviors.setup { ctx =>
      Behaviors.withTimers { (timers: TimerScheduler[Message]) =>
        implicit val context: ActorContext[Message] = ctx
        val inputsKey = ServiceKey[Message]("inputs")
        val outputsKey = ServiceKey[Message]("outputs")
        timers.startTimerWithFixedDelay("inputsTimer", GetInputs(), FiniteDuration(3, SECONDS))
        timers.startTimerWithFixedDelay("outputsTimer", GetOutputs(), FiniteDuration(5, SECONDS))
        implicit val listingResponseAdapter: ActorRef[Receptionist.Listing] =
          context.messageAdapter[Receptionist.Listing] {
            case inputsKey.Listing(listings) => InputsListing(listings)
            case outputsKey.Listing(listings) => OutputListing(listings)
          }
        context.system.receptionist.tell(Receptionist.register(ServiceKey[InputServiceMsg]("supervisor"), context.self))
        Supervisor().behavior(HashMap())
      }
    }

  private def behavior(camerasStatus:HashMap[Info, ChildStatuses])(implicit ctx: ActorContext[Message], adapter: ActorRef[Receptionist.Listing]): Behavior =
    Behaviors.setup(ctx => {
      Behaviors.receiveMessage {
        case GetOutputs() =>
          val outputsKey = ServiceKey[Message]("outputs")
          ctx.system.receptionist.tell(Receptionist.find(outputsKey, adapter))
          Supervisor().behavior(camerasStatus)

        case GetInputs() =>
          val inputsKey = ServiceKey[Message]("inputs")
          ctx.system.receptionist.tell(Receptionist.find(inputsKey, adapter))
          Supervisor().behavior(camerasStatus)

        case OutputListing(listing) =>
          listing.foreach(_ ! CameraMap(ctx.self, camerasStatus))
          Supervisor().behavior(camerasStatus)

        case Pong(info) =>
          Supervisor().behavior(camerasStatus)

        case InputsListing(listing) =>
          listing.foreach(_ ! GetChildStatus(ctx.self))
          Supervisor().behavior(camerasStatus.filter((k, v) => listing.contains(k.self)))

        case ChildStatus(info, status) =>
          Supervisor().behavior(camerasStatus + (info -> status))
      }
    })