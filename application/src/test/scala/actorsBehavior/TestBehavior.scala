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

import actor.{CameraManager, ReachableActor}
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.flatspec.AnyFlatSpec
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.stream.Materializer
import akka.stream.javadsl.StreamRefs
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import message.{ChildStatus, Config, ConfigServiceSuccess, GetChildStatus, Input, InputServiceFailure, InputServiceSuccess, Message, Output, Ping, PingServiceMsg, Pong, Subscribe, SubscribeServiceFailure, SubscribeServiceSuccess, SwitchToCamera}
import utils.ChildStatuses.{Idle, Running}
import utils.{ActorTypes, ChildStatuses, Info, InputServiceErrors, StandardChildProcessCommands}

import scala.collection.immutable.Queue
import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration

class TestBehavior extends AnyFlatSpec:

  "A ReachableActor" should "be reachable" in testReachableBehavior()
  "A CameraManager" should "instantiate a child process and reset it when a new ConfigMessage is received and manage its I/O" in testCameraManagerBehavior()

  def testReachableBehavior(): Unit =
    class ConcreteReachableActor extends ReachableActor:
      def create():Behavior[Message] =
        Behaviors.setup { context =>
          implicit val ctx: ActorContext[Message] = context
          implicit val mat: Materializer = Materializer(ctx.system)
          this.behavior(super.setActorInfo(Info()))
        }
      def behavior(info:Info)(implicit materializer: Materializer, ctx:ActorContext[Message]): Behavior[Message] =
        Behaviors.setup { ctx =>
          Behaviors.receiveMessagePartial(getReachableBehavior(info))
        }
    val testKit: ActorTestKit = ActorTestKit()
    val pinger = testKit.createTestProbe[Message]()
    val exampleInfo = Info()
    val reachableActor = new ConcreteReachableActor()
    val actorRef = testKit.spawn(reachableActor.create())
    actorRef ! Ping(pinger.ref)
    pinger.expectMessage(Pong(exampleInfo.setSelfRef(actorRef)))

  def testCameraManagerBehavior(): Unit =
    val powershellCommand: String = if(System.getProperty("os.name").toLowerCase().contains("win")) "powershell" else "pwsh"
    val testKit: ActorTestKit = ActorTestKit()
    val outputProbe = testKit.createTestProbe[Message]()
    val inputProbe = testKit.createTestProbe[Message]()
    val cameraManager = testKit.spawn(CameraManager(9999).create())
    val expectedInfo = Info(cameraManager.ref, Set(), ActorTypes.CameraManager)
    val pingProbe = testKit.createTestProbe[PingServiceMsg]()
    val forwarder: ActorRef[Message] = testKit.spawn(
      Behaviors.setup{ ctx =>
        val sink =  Sink.foreach[ByteString](bs => outputProbe ! Output(bs.utf8String.strip()))
        Behaviors.receiveMessage {
          case SwitchToCamera(cameraRef) =>
            cameraRef ! Subscribe(ctx.self, sink.runWith(StreamRefs.sinkRef())(Materializer(ctx.system)))
            Behaviors.same

          case SubscribeServiceSuccess(info) =>
            outputProbe ! SubscribeServiceSuccess(info)
            Behaviors.same

          case SubscribeServiceFailure(info, cause) =>
            outputProbe ! SubscribeServiceFailure(info, cause)
            Behaviors.same
        }
      }
    )

    def sendSuccessfullConfig(arg: String): Unit =
      cameraManager ! Config(inputProbe.ref, Queue(powershellCommand + " -ExecutionPolicy Bypass -File src/test/powershell/testCameraManagerScript.ps1 ", arg))
      inputProbe.expectMessageType[ConfigServiceSuccess]
      Thread.sleep(3000)

    def sendSuccessfullInput(input: String): Unit =
      cameraManager ! Input(inputProbe.ref, input)
      inputProbe.expectMessage(InputServiceSuccess(expectedInfo))
      if(input != StandardChildProcessCommands.Kill.command) outputProbe.expectMessage(FiniteDuration(10, duration.SECONDS), Output(input))

    def requestChildStatus(expectedStatus: ChildStatuses): Unit =
      cameraManager ! GetChildStatus(outputProbe.ref)
      outputProbe.expectMessage(ChildStatus(expectedInfo, expectedStatus))

    outputProbe.expectNoMessage(FiniteDuration(5, duration.SECONDS))
    requestChildStatus(Idle)
    sendSuccessfullConfig("firstRunArg")
    forwarder ! SwitchToCamera(cameraManager)
    outputProbe.expectMessage(SubscribeServiceSuccess(expectedInfo))
    outputProbe.expectMessage(FiniteDuration(10, duration.SECONDS), Output("firstRunArg"))

    requestChildStatus(Running)
    sendSuccessfullInput("runtimeArg1")
    sendSuccessfullConfig("secondRunArg")
    outputProbe.expectMessage(SubscribeServiceSuccess(expectedInfo))
    outputProbe.expectMessage(FiniteDuration(10, duration.SECONDS), Output("secondRunArg"))
    sendSuccessfullInput("runtimeArg2")
    sendSuccessfullInput(StandardChildProcessCommands.Kill.command)
    cameraManager ! Input(inputProbe.ref, "anything")
    inputProbe.expectMessage(FiniteDuration(10, duration.SECONDS), InputServiceFailure(InputServiceErrors.MissingChild))
    requestChildStatus(Idle)
    forwarder ! SwitchToCamera(cameraManager)
    outputProbe.expectMessage(SubscribeServiceFailure(expectedInfo, InputServiceErrors.MissingChild))
    cameraManager ! Ping(pingProbe.ref)
    pingProbe.expectMessage(Pong(Info(cameraManager.ref, Set(), ActorTypes.CameraManager)))








