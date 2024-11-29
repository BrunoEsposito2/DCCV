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

package actorsBehavior

import actor.{AbstractService, CameraManager, ConfigureClientSink, Supervisor}
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import message.{CameraMap, *}
import org.scalatest.flatspec.AnyFlatSpec
import utils.ChildStatuses.{Idle, Running}
import utils.{ActorTypes, Info, StandardChildProcessCommands}

import java.util.concurrent.TimeUnit
import scala.collection.immutable.Queue
import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration

class TestBehavior extends AnyFlatSpec:

  "The AbstractService" should "provide an actor behavior for dealing with all the others system's actors in an abstract way, " +
    "letting the user implement the operations of the concrete client operations without change the actor behavior " in testAbstractServiceBehavior()
  "The Supervisor" should "mantain an updated global view of all the actors deployed in the system" in testSupervisorBehavior()

  private class ConcreteService(probeRef: ActorRef[Message]) extends AbstractService:
    override def onMessage(msg:Message):Unit =
      msg match
        case msg: SwitchToCamera => probeRef ! msg
        case msg: ConfigServiceSuccess => probeRef ! msg
        //every message operation can be implemented in this case match. For this test the implementation is the same for all message types
        case _ => probeRef ! msg

  def testAbstractServiceBehavior(): Unit =
    val testKit: ActorTestKit = ActorTestKit()
    val probe1 = testKit.createTestProbe[Message]()
    val client1 = testKit.spawn(AbstractService(new ConcreteService(probe1.ref)))
    val probe2 = testKit.createTestProbe[Message]()
    val client2 = testKit.spawn(AbstractService(new ConcreteService(probe2.ref)))
    val outputProbe = testKit.createTestProbe[Message]()
    val camera_A = testKit.spawn(CameraManager(9999).create())
    val expectedCamera_A_info = Info().setSelfRef(camera_A).setActorType(ActorTypes.CameraManager)
    val camera_B = testKit.spawn(CameraManager(9999).create())
    val expectedCamera_B_info = Info().setSelfRef(camera_B).setActorType(ActorTypes.CameraManager)


    //configure clients consumers
    client1 ! ConfigureClientSink(byteString => {
      println("c1: "+byteString.utf8String.strip())
      probe1 ! Output("c1: "+byteString.utf8String.strip())
    })

    client2 ! ConfigureClientSink(byteString => {
      Thread.sleep(1000) //client2 is delayed for testing purposes
      println("c2: " + byteString.utf8String.strip())
      probe2 ! Output("c2: " + byteString.utf8String.strip())
    })

    //client1 starts camera_A
    val powershellCommand: String = if (System.getProperty("os.name").toLowerCase().contains("win")) "powershell" else "pwsh"
    val configCommand = Queue(powershellCommand + " -ExecutionPolicy Bypass -File ../application/src/test/powershell/testCameraManagerScript.ps1 ", "configArg")

    AbstractService.configureCamera(camera_A, client1, configCommand)
    probe1.expectMessageType[ConfigServiceSuccess]
    Thread.sleep(5000)

    //client1 switch to camera_A and tries an input
    client1 ! SwitchToCamera(camera_A)
    probe1.expectMessage(SwitchToCamera(camera_A))
    probe1.expectMessage(SubscribeServiceSuccess(expectedCamera_A_info))
    camera_A ! Input(client1, "client1_input")
    probe1.expectMessage(InputServiceSuccess(expectedCamera_A_info))
    probe1.expectMessage(Output("c1: configArg"))
    probe1.expectMessage(Output("c1: client1_input"))

    //client2 switch to camera_A
    client2 ! SwitchToCamera(camera_A)
    probe2.expectMessage(SwitchToCamera(camera_A))
    probe2.expectMessage(SubscribeServiceSuccess(expectedCamera_A_info))
    Thread.sleep(1000)

    //client1 tries another input (both clients receives it)
    camera_A ! Input(client1, "client1_input")
    probe1.expectMessage(InputServiceSuccess(expectedCamera_A_info))
    probe1.expectMessage(Output("c1: client1_input"))
    probe2.expectMessage(FiniteDuration(20, TimeUnit.SECONDS), Output("c2: client1_input"))

    //client1 defines a new consumer function at runtime and send another input
    client1 ! ConfigureClientSink(byteString => {
      println("modified_c1: " + byteString.utf8String.strip())
      probe1 ! Output("modified_c1: " + byteString.utf8String.strip())
    })
    probe1.expectMessage(SwitchToCamera(camera_A))
    Thread.sleep(1000)
    camera_A ! Input(client1, "client1_input_2")
    probe1.expectMessage(SubscribeServiceSuccess(expectedCamera_A_info))
    probe1.expectMessage(InputServiceSuccess(expectedCamera_A_info))
    probe1.expectMessage(Output("modified_c1: client1_input_2"))
    probe2.expectMessage(FiniteDuration(20, TimeUnit.SECONDS), Output("c2: client1_input_2"))

    //client2 kills the process in camera A, then config and switch to camera B
    camera_A ! Input(client2, StandardChildProcessCommands.Kill.command)
    probe2.expectMessage(InputServiceSuccess(expectedCamera_A_info))
    probe1.expectNoMessage(FiniteDuration(2, TimeUnit.SECONDS))
    AbstractService.configureCamera(camera_B, client2, configCommand)
    probe2.expectMessageType[ConfigServiceSuccess]
    client2 ! SwitchToCamera(camera_B)
    probe2.expectMessage(SwitchToCamera(camera_B))
    probe2.expectMessage(SubscribeServiceSuccess(expectedCamera_B_info))
    camera_B ! Input(client2, "client2_input_1")
    probe2.expectMessage(InputServiceSuccess(expectedCamera_B_info))
    probe2.expectMessage(FiniteDuration(20, TimeUnit.SECONDS), Output("c2: configArg"))
    probe2.expectMessage(FiniteDuration(20, TimeUnit.SECONDS), Output("c2: client2_input_1"))
    probe1.expectNoMessage(FiniteDuration(2, TimeUnit.SECONDS))
    camera_B ! Input(client2, StandardChildProcessCommands.Kill.command)
    probe2.expectMessage(InputServiceSuccess(expectedCamera_B_info))

  def testSupervisorBehavior(): Unit =
    val testKit: ActorTestKit = ActorTestKit()
    val clientsProbe = testKit.createTestProbe[Message]()
    val client1 = testKit.spawn(AbstractService(new ConcreteService(clientsProbe.ref)))
    val client2 = testKit.spawn(AbstractService(new ConcreteService(clientsProbe.ref)))
    val camera_A = testKit.spawn(CameraManager(9999).create())
    val expectedCamera_A_info = Info().setSelfRef(camera_A).setActorType(ActorTypes.CameraManager)
    val camera_B = testKit.spawn(CameraManager(9999).create())
    val expectedCamera_B_info = Info().setSelfRef(camera_B).setActorType(ActorTypes.CameraManager)
    val supervisor = testKit.spawn(Supervisor().create())
    Thread.sleep(5000)
    clientsProbe.expectMessage(FiniteDuration(20, TimeUnit.SECONDS), CameraMap(supervisor, Map(expectedCamera_A_info -> Idle, expectedCamera_B_info -> Idle)))
    clientsProbe.expectMessage(FiniteDuration(20, TimeUnit.SECONDS), CameraMap(supervisor, Map(expectedCamera_A_info -> Idle, expectedCamera_B_info -> Idle)))
    val powershellCommand: String = if (System.getProperty("os.name").toLowerCase().contains("win")) "powershell" else "pwsh"
    val configCommand = Queue(powershellCommand + " -ExecutionPolicy Bypass -File ../application/src/test/powershell/testCameraManagerScript.ps1 ", "configArg")
    camera_A ! Config(clientsProbe.ref, configCommand)
    clientsProbe.expectMessage(ConfigServiceSuccess(expectedCamera_A_info))
    clientsProbe.expectMessage(FiniteDuration(20, TimeUnit.SECONDS), CameraMap(supervisor, Map(expectedCamera_A_info -> Running, expectedCamera_B_info -> Idle)))
    clientsProbe.expectMessage(FiniteDuration(20, TimeUnit.SECONDS), CameraMap(supervisor, Map(expectedCamera_A_info -> Running, expectedCamera_B_info -> Idle)))
    camera_A ! Input(clientsProbe.ref, StandardChildProcessCommands.Kill.command)

