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

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import com.mongodb.client.MongoCollection
import database.MongoDBDriver
import message.{CameraMap, Message, SwitchToCamera}
import org.bson.Document
import utils.{ChildStatuses, Info}

import scala.util.Random

object DBCoordinator:
  def apply(): Behavior = new DBCoordinator().create()

private class DBCoordinator:
  private def create(): Behavior =
    var eventualCollection:Option[MongoCollection[Document]] = Option.empty
    val mongo = MongoDBDriver()
    while(eventualCollection.isEmpty)
      eventualCollection = mongo.connect()
      Thread.sleep(5000)

    Behaviors.setup {
      ctx =>
        ctx.system.receptionist.tell(Receptionist.register(ServiceKey[Message]("outputs"), ctx.self))
        this.behavior(Set.empty, eventualCollection.get)
    }
  private def behavior(previousCameraSet: Set[Info], mongoCollection: MongoCollection[Document]): Behavior =
    Behaviors.setup(ctx => {
      Behaviors.receiveMessage {
        case CameraMap(replyTo, map) =>
          val newSet = map
            .filter((k,v) => v.equals(ChildStatuses.Running)).keySet
          newSet
            .filter(!previousCameraSet.contains(_))
            .foreach(newRunningCamera =>
              ctx.spawn(
                DBWriter(mongoCollection, newRunningCamera.self.toString), "DBWriter_" + Random.nextInt(100000))
              ! SwitchToCamera(newRunningCamera.self)
          )
          this.behavior(previousCameraSet union newSet, mongoCollection)
      }
    })