package actorsBehavior

import actor.{CameraManager, ReachableActor}
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.flatspec.AnyFlatSpec
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.Materializer
import message.{CameraOutputStreamSource, Config, ConfigServiceSuccess, GetSourceRef, Input, InputServiceFailure, InputServiceSuccess, Message, Output, OutputServiceMsg, Ping, PingServiceMsg, Pong}
import utils.{Info, InputServiceErrors, StandardChildProcessCommands}

import scala.collection.immutable.Queue
import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration

class TestBehavior extends AnyFlatSpec:

  "A ReachableActor" should "be reachable" in testReachableBehavior()
  "A CameraManager" should "instantiate a child process and reset it when a new ConfigMessage is received and manage its I/O" in testCameraManagerBehavior()

  def testReachableBehavior(): Unit =
    val testKit: ActorTestKit = ActorTestKit()
    val pinger = testKit.createTestProbe[Message]()
    val exampleInfo = Info()
    val actorRef = testKit.spawn(ReachableActor(exampleInfo).create())
    actorRef ! Ping(pinger.ref)
    pinger.expectMessage(Pong(exampleInfo.setSelfRef(actorRef)))

  def testCameraManagerBehavior(): Unit =
    val powershellCommand: String = if(System.getProperty("os.name").toLowerCase().contains("win")) "powershell" else "pwsh"
    val testKit: ActorTestKit = ActorTestKit()
    val outputProbe = testKit.createTestProbe[OutputServiceMsg]()
    val inputProbe = testKit.createTestProbe[Message]()
    val cameraManager = testKit.spawn(CameraManager(9999).create())
    val expectedInfo = Info(cameraManager.ref, Set(), "CameraManager")
    val pingProbe = testKit.createTestProbe[PingServiceMsg]()
    val forwarder: ActorRef[Message] = testKit.spawn(
      Behaviors.setup{ ctx =>
        Behaviors.receiveMessage {
          case CameraOutputStreamSource(info, sourceRef) =>
            implicit val mat:Materializer = Materializer(ctx.system)
            sourceRef.source.runForeach(bytes => {
              outputProbe ! Output(bytes.utf8String.strip())
            })
            Behaviors.same
        }
      }
    )

    def sendSuccessfullConfig(arg: String): Unit =
      cameraManager ! Config(inputProbe.ref, Queue(powershellCommand + " -ExecutionPolicy Bypass -File src/test/powershell/testCameraManagerScript.ps1 ", arg))
      inputProbe.expectMessageType[ConfigServiceSuccess]
      cameraManager ! GetSourceRef(forwarder)
      Thread.sleep(3000)
      outputProbe.expectMessage(FiniteDuration(10, duration.SECONDS), Output(arg))

    def sendSuccessfullInput(input: String): Unit =
      cameraManager ! Input(inputProbe.ref, input)
      inputProbe.expectMessage(InputServiceSuccess(expectedInfo))
      if(input != StandardChildProcessCommands.Kill.command) outputProbe.expectMessage(FiniteDuration(10, duration.SECONDS), Output(input))

    outputProbe.expectNoMessage(FiniteDuration(5, duration.SECONDS))
    sendSuccessfullConfig("firstRunArg")
    sendSuccessfullInput("runtimeArg1")
    sendSuccessfullConfig("secondRunArg")
    sendSuccessfullInput("runtimeArg2")
    sendSuccessfullInput(StandardChildProcessCommands.Kill.command)
    cameraManager ! Input(inputProbe.ref, "anything")
    inputProbe.expectMessage(FiniteDuration(10, duration.SECONDS), InputServiceFailure(InputServiceErrors.MissingChild))
    cameraManager ! Ping(pingProbe.ref)
    pingProbe.expectMessage(Pong(Info(cameraManager.ref, Set(), "CameraManager")))








