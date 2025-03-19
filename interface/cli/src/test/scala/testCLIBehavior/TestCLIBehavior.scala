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

package testCLIBehavior

import actor.{CLIClient, CameraManager, Supervisor}
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors

import message.*
import org.scalatest.flatspec.AnyFlatSpec
import parser.{CLIMessage, CLIParser}
import utils.ChildStatuses.Idle
import utils.{ActorTypes, ChildStatuses, Info, StandardChildProcessCommands}

import java.io.{BufferedReader, InputStreamReader, PipedInputStream, PipedOutputStream, PrintStream, PrintWriter}

class TestCLIBehavior extends AnyFlatSpec:

  "The command line interface" should "behave as expected using the available commands" in testCLIBehavior()
  private case class CustomMessage(testString: String) extends Message

  def testCLIBehavior(): Unit =
    //redirect stdin
    val pipeOut = new PipedOutputStream()
    val pipeIn = new PipedInputStream(pipeOut)
    val writer = new PrintWriter(pipeOut, true) // true for auto-flush

    //redirect stdout
    val outContent = new PipedOutputStream()
    val reader = new BufferedReader(new InputStreamReader(new PipedInputStream(outContent)))
    val defaultOut = System.out
    System.setOut(new PrintStream(outContent))

    //start system actors
    val testKit: ActorTestKit = ActorTestKit()
    val camera = testKit.spawn(CameraManager(9999).create())
    val camera2 = testKit.spawn(CameraManager(9998).create())
    val expectedInfo = Info(camera, Set(), ActorTypes.CameraManager)
    val cliClient = testKit.spawn(CLIClient().create())
    val supervisor = testKit.spawn(Supervisor().create())
    Thread.sleep(2000)

    //start CLI
    val dummy: ActorRef[Message] = testKit.spawn(Behaviors.setup(ctx =>
      Console.withIn(pipeIn) {
        new CLIParser(cliClient).run()
      }
      Behaviors.same
    ))

    //define testing shortcuts
    def validateCLIOutput(expectedOutput: String): Unit =
      val expected: String =/* CLIMessage.InsertCommand.message + */expectedOutput
      expected.lines().forEach(expectedLine => assertResult(expectedLine)(reader.readLine()))

    // Write to the runnable's stdin
    def write(input: String): Unit = writer.println(input)
    Thread.sleep(2000)

    //try random wrong command
    write("-plplpl")
    validateCLIOutput(CLIMessage.CommandNotFound.message)

    //try -help
    write("-help")
    validateCLIOutput(CLIMessage.getHelpMessage)

    //give time to the cluster to reach all initialized actors and get the camera list
    Thread.sleep(5000)
    write("-showCameras")
    var cameraList = "#" + camera + " : " + ChildStatuses.Idle + "#"+camera2 + " : " + ChildStatuses.Idle
    cameraList = cameraList.stripMargin('#')
    validateCLIOutput(cameraList)

    //try configure a C++ process
    val powershellCommand: String = if(System.getProperty("os.name").toLowerCase().contains("win")) "powershell" else "pwsh"

    write("-config " + camera.toString + " " + powershellCommand + " -ExecutionPolicy Bypass -File ../../application/src/test/powershell/testCameraManagerScript.ps1 cliParameter")
    Thread.sleep(1000)
    validateCLIOutput(camera.toString + " succesfully configured.")
    Thread.sleep(5000)

    //try to subscribe to that camera manager and said process
    write("-subscribe " + camera.toString)

    //get the first output from process (init parameter)
    validateCLIOutput("output received: cliParameter")

    //try the echo process with the -input command
    write("-input testCLI1")
    validateCLIOutput("output received: testCLI1")

    //try hiding the stream and then showing it, ensuring that the hided results are not the next element in the CLI stream
    write("-hideStream")
    Thread.sleep(10000)
    write("-input testCLI99")
    for i <- 0 to 4 do
      if(i < 2) write("-input testCLI" + i)
      else
        Thread.sleep(4000)
        write("-showStream")
        Thread.sleep(4000)
        write("-input testCLI" + i)
        validateCLIOutput("output received: testCLI" + i)

    //close C++ process
    write("-input " + StandardChildProcessCommands.Kill.command)
    Thread.sleep(1000) // Give thread time to process

    writer.close()
    pipeIn.close()
    pipeOut.close()