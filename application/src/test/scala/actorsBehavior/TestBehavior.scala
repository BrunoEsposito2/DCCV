package actorsBehavior

import actor.{CameraManager, ReachableActor}
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.flatspec.AnyFlatSpec
import akka.actor.typed.ActorRef
import message.{ConfigMsg, InputMsg, Message, Output, OutputServiceMsg, Ping, PingServiceMsg, Pong}
import utils.Info

import java.util.concurrent.TimeUnit
import scala.collection.immutable.Queue
import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration

class TestBehavior extends AnyFlatSpec:

  "A ReachableActor" should "be reachable" in testPong()
  "A CameraManager" should "instantiate a child process and reset it when a new ConfigMessage is received and manage its I/O" in testCameraManagerBehavior()

  val testKit: ActorTestKit = ActorTestKit()
  val powershellCommand: String = if(System.getProperty("os.name").toLowerCase().contains("win")) "powershell" else "pwsh"
  def testPong(): Unit =
    val pinger = testKit.createTestProbe[Message]()
    val exampleInfo = Info()
    val actorRef = testKit.spawn(ReachableActor(exampleInfo).behavior())
    actorRef ! Ping(pinger.ref)
    pinger.expectMessage(Pong(exampleInfo.setSelfRef(actorRef)))

  def testCameraManagerBehavior():Unit =
    val probe = testKit.createTestProbe[OutputServiceMsg]()
    val cameraManager = testKit.spawn(CameraManager(Info(), null, Option.empty, 9999, probe.ref).behavior())
    probe.expectNoMessage(FiniteDuration(5, duration.SECONDS))
    cameraManager ! ConfigMsg(Queue(powershellCommand + " -ExecutionPolicy Bypass -File src/test/powershell/testCameraManagerScript.ps1 ", "firstRunArg"))
    Thread.sleep(2000)
    probe.expectMessage(Output("firstRunArg"))
    cameraManager ! InputMsg("runtimeArg1")
    probe.expectMessage(FiniteDuration(15, TimeUnit.SECONDS), Output("runtimeArg1"))
    cameraManager ! ConfigMsg(Queue(powershellCommand + " -ExecutionPolicy Bypass -File src/test/powershell/testCameraManagerScript.ps1 ", "secondRunArg"))
    probe.expectMessage(FiniteDuration(15, TimeUnit.SECONDS), Output("secondRunArg"))
    cameraManager ! InputMsg("runtimeArg2")
    probe.expectMessage(Output("runtimeArg2"))
    cameraManager ! InputMsg("k")
    val pingProbe = testKit.createTestProbe[PingServiceMsg]()
    cameraManager ! Ping(pingProbe.ref)
    pingProbe.expectMessage(Pong(Info(cameraManager.ref, Set(probe.ref), "CameraManager")))




