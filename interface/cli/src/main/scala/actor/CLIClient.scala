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

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.ByteString
import message.{CameraMap, Input, Message, OutputServiceMsg}
import parser.CLIMessage
import utils.{ChildStatuses, Info}

import scala.io.StdIn.{readInt, readLine}
import scala.util.control.Breaks.break

case class HideStream() extends OutputServiceMsg
case class ShowStream() extends OutputServiceMsg
case class SendInput(input: String) extends OutputServiceMsg

object CLIClient:
  def apply(): CLIClient = new CLIClient()

private class CLIClient extends AbstractClient:
  private var cameraMap: Map[Info, ChildStatuses] = Map.empty

  override def onMessage(msg: Message, clientInfo: Info): Unit =
    msg match
      case CameraMap(replyTo, map) =>
        cameraMap = map
      case HideStream() => clientInfo.self ! ConfigureClientSink(bs => print(""))
      case ShowStream() => clientInfo.self ! ConfigureClientSink(this.startingSinkFunction())
      case SendInput(input) => 
        if(clientInfo.linkedActors.isEmpty) 
          println(CLIMessage.InputError.message)
        else clientInfo.linkedActors.head ! Input(clientInfo.self, input)
      case _ => println("message received: "+msg.toString)

  override def startingSinkFunction(): ByteString => Unit =
    bs => CLIMessage.OutputPrefix.message + println(bs.utf8String.strip())

  def getCameraMap: Map[Info, ChildStatuses] = cameraMap.toList.sorted((entry1, entry2) => entry1._1.self.toString.compareTo(entry2._1.self.toString)).toMap
