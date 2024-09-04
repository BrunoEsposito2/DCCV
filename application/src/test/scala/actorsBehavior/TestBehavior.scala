package actorsBehavior

import actor.{CameraManager, OutputSupervisor, ReachableActor}
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.flatspec.AnyFlatSpec
import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import message.{ConfigMsg, InputMsg, Message, Output, OutputServiceMsg, Ping, PingServiceMsg, Pong}
import utils.Info

import java.util.concurrent.TimeUnit
import scala.collection.immutable.Queue
import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration

class TestBehavior extends AnyFlatSpec:

  "A ReachableActor" should "be reachable" in testPong()
  "A CameraManager" should "instantiate a child process and reset it when a new ConfigMessage is received and manage its I/O" in testCameraManagerBehavior()
  "An OutputSupervisor" should "connect all InputServiceMsg actors to an OutputServiceMsg actor if their initialized outputRef becomes invalid" in testOutputSupervisorBehavior()

  val testKit: ActorTestKit = ActorTestKit()
  val powershellCommand: String = if(System.getProperty("os.name").toLowerCase().contains("win")) "powershell" else "pwsh"

  def testPong(): Unit =
    val pinger = testKit.createTestProbe[Message]()
    val exampleInfo = Info()
    val actorRef = testKit.spawn(ReachableActor(exampleInfo).create())
    actorRef ! Ping(pinger.ref)
    pinger.expectMessage(Pong(exampleInfo.setSelfRef(actorRef)))

  def testCameraManagerBehavior(): Unit =
    val probe = testKit.createTestProbe[OutputServiceMsg]()
    val cameraManager = testKit.spawn(CameraManager(9999, probe.ref).create())
    probe.expectNoMessage(FiniteDuration(5, duration.SECONDS))
    cameraManager ! ConfigMsg(Queue(powershellCommand + " -ExecutionPolicy Bypass -File src/test/powershell/testCameraManagerScript.ps1 ", "firstRunArg"))
    Thread.sleep(2000)
    probe.expectMessage( FiniteDuration(10, duration.SECONDS), Output("firstRunArg"))
    cameraManager ! InputMsg("runtimeArg1")
    probe.expectMessage(FiniteDuration(10, TimeUnit.SECONDS), Output("runtimeArg1"))
    cameraManager ! ConfigMsg(Queue(powershellCommand + " -ExecutionPolicy Bypass -File src/test/powershell/testCameraManagerScript.ps1 ", "secondRunArg"))
    probe.expectMessage(FiniteDuration(10, TimeUnit.SECONDS), Output("secondRunArg"))
    cameraManager ! InputMsg("runtimeArg2")
    probe.expectMessage(Output("runtimeArg2"))
    cameraManager ! InputMsg("k")
    val pingProbe = testKit.createTestProbe[PingServiceMsg]()
    cameraManager ! Ping(pingProbe.ref)
    pingProbe.expectMessage(Pong(Info(cameraManager.ref, Set(probe.ref), "CameraManager")))

  def testOutputSupervisorBehavior(): Unit =
    val pingProbe = testKit.createTestProbe[PingServiceMsg]()
    val deadProbe = testKit.createTestProbe[OutputServiceMsg]()
    val cameraManager = testKit.spawn(CameraManager(9999, deadProbe.ref).create())
    deadProbe.stop()
    val supervisor = testKit.spawn(OutputSupervisor().create())
    val validOutput = testKit.createTestProbe[OutputServiceMsg]()
    Thread.sleep(15000)
    //cameraManager outputRef is not changed because the supervisor's output ref set is empty
    cameraManager ! Ping(pingProbe.ref)
    pingProbe.expectMessage(Pong(Info(cameraManager.ref, Set(deadProbe.ref), "CameraManager")))
    //make the validOutput actor visible to the supervisor
    testKit.system.receptionist.tell(Receptionist.register(ServiceKey[OutputServiceMsg]("outputs"), validOutput.ref))
    println(pingProbe.ref.toString +" " +validOutput.ref)
    //wait 15 seconds to let the Supervisor link the orphaned CameraManager to the new validOutput actor
    Thread.sleep(15000)
    cameraManager ! Ping(pingProbe.ref)
    pingProbe.expectMessage(Pong(Info(cameraManager.ref, Set(validOutput.ref), "CameraManager")))








