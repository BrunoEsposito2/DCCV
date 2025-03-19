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

package parser

import actor.{CLIClient, HideStream, SendInput, ShowStream, Terminate}
import akka.actor.typed.{ActorRef, ActorSystem}
import message.{Config, Message, SwitchToCamera}
import utils.ChildStatuses.{Idle, Running}
import utils.{ChildStatuses, Info}
import CLICommand.*

import scala.io.StdIn
import scala.io.StdIn.readLine


class CLIParser(clientRef: ActorRef[Message]) extends Runnable:
  override def run():Unit =

    def validateCamera(cameraName: String): Option[(Info, ChildStatuses)] =
      val matchingCameras = CLIClient.getCameraMap.filter((info, cs) => info.self.toString == cameraName).toList
      matchingCameras.size match
        case 1 => Option(matchingCameras.head)
        case _ => Option.empty
        
    while (true)
      val insertedInputs = scala.io.StdIn.readLine()
      val inputs = insertedInputs.strip().split(" ").toSeq
      inputs.size match
        case 1 =>
          inputs.head match
            case Help.command => println(CLIMessage.getHelpMessage)
            case Quit.command => clientRef ! Terminate(); System.exit(0)
            case CLICommand.HideStream.command =>
              clientRef ! HideStream()
            case CLICommand.ShowStream.command =>
              clientRef ! ShowStream()
            case ShowCameras.command =>
              var cameraList = ""
              CLIClient.getCameraMap.foreach(entry => cameraList += "#"+entry._1.self.toString + " : "+ entry._2.status)
              println(cameraList.stripMargin('#'))
            case _ => println(CLIMessage.CommandNotFound.message)
        case 2 =>
          inputs.head match
            case Subscribe.command =>
              val camera = validateCamera(inputs(1)) 
              if(camera.nonEmpty) camera.get._2 match
                case Idle => println(CLIMessage.CantSubscribeToIdleCamera.message)
                case Running => 
                  clientRef ! SwitchToCamera(camera.get._1.self)
              else println(CLIMessage.CameraNotFound.message)
            case Input.command =>
              clientRef ! SendInput(inputs(1))
        case _ =>
          inputs.size match
            case 0 => println(CLIMessage.NoCommandTyped.message)
            case _ => 
              if(inputs.head == CLICommand.Config.command)
                val camera = validateCamera(inputs(1))
                if(camera.isEmpty) println(CLIMessage.CameraNotFound.message)
                else
                  camera.get._1.self ! Config(clientRef, inputs.drop(2).to(collection.immutable.Queue))
              else println(CLIMessage.CommandNotFound.message)