package actorsBehavior

import actor.{CameraManager, OutputSupervisor, ReachableActor}
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import org.scalatest.flatspec.AnyFlatSpec
import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import message.{Config, Input, InputServiceFailure, InputServiceSuccess, Message, Output, OutputServiceMsg, Ping, PingServiceMsg, Pong}
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
    val outputProbe = testKit.createTestProbe[OutputServiceMsg]()
    val inputProbe = testKit.createTestProbe[Message]()
    val cameraManager = testKit.spawn(CameraManager(9999, outputProbe.ref).create())
    val expectedInfo = Info(cameraManager.ref, Set(outputProbe.ref), "CameraManager")
    val pingProbe = testKit.createTestProbe[PingServiceMsg]()

    def sendSuccessfullConfig(arg: String): Unit =
      cameraManager ! Config(inputProbe.ref, Queue(powershellCommand + " -ExecutionPolicy Bypass -File src/test/powershell/testCameraManagerScript.ps1 ", arg))
      inputProbe.expectMessage(InputServiceSuccess(expectedInfo))
      outputProbe.expectMessage(FiniteDuration(10, duration.SECONDS), Output(arg))

    def sendSuccessfullInput(input: String): Unit =
      cameraManager ! Input(inputProbe.ref, input)
      inputProbe.expectMessage(InputServiceSuccess(expectedInfo))
      if(input != "k")outputProbe.expectMessage(FiniteDuration(10, duration.SECONDS), Output(input))

    outputProbe.expectNoMessage(FiniteDuration(5, duration.SECONDS))
    sendSuccessfullConfig("firstRunArg")
    sendSuccessfullInput("runtimeArg1")
    sendSuccessfullConfig("secondRunArg")
    sendSuccessfullInput("runtimeArg2")
    sendSuccessfullInput("k")
    cameraManager ! Input(inputProbe.ref, "k")
    inputProbe.expectMessage(InputServiceFailure("Child process undefined: operation aborted for potential unwanted side effects."))
    cameraManager ! Ping(pingProbe.ref)
    pingProbe.expectMessage(Pong(Info(cameraManager.ref, Set(outputProbe.ref), "CameraManager")))

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








