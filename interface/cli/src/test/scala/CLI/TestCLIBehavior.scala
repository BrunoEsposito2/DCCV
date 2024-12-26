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

import actor.{AbstractClient, CameraManager, ConfigureClientSink, Supervisor}
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.util.ByteString
import message.*
import org.scalatest.flatspec.AnyFlatSpec
import parser.CLIParser
import utils.ChildStatuses.{Idle, Running}
import utils.{ActorTypes, Info, StandardChildProcessCommands}

import java.io.{BufferedInputStream, BufferedReader, InputStream, InputStreamReader, OutputStreamWriter, PipedInputStream, PipedOutputStream, PrintStream, PrintWriter}
import java.util.concurrent.TimeUnit
import scala.collection.immutable.Queue
import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration
import scala.io.StdIn

class TestCLIBehavior extends AnyFlatSpec:

  "The AbstractService" should "provide an actor behavior for dealing with all the others system's actors in an abstract way, " in testAbstractServiceBehavior()
  private case class CustomMessage(testString: String) extends Message

  def testAbstractServiceBehavior(): Unit =
    val pipeOut = new PipedOutputStream()
    val pipeIn = new PipedInputStream(pipeOut)
    val writer = new PrintWriter(pipeOut, true) // true for auto-flush

    //stdout
    val outContent = new PipedOutputStream()
    val reader = new BufferedReader(new InputStreamReader(new PipedInputStream(outContent)))
    val defaultOut = System.out

    System.setOut(new PrintStream(outContent))
    Console.withIn(pipeIn) {
        new Thread(new CLIParser()).start()
    }
    //val thread = new Thread(new CLIParser())
    Thread.sleep(2000)

    // Write to the runnable's stdin
    writer.println("-plplpl")
    val preString = "enter command:"
    assert(reader.readLine() == preString+"Command not found: type -help to get the valid commands list.")
    writer.println("")
    assert(reader.readLine() == preString+"no command typed: use -help to get the valid commands list")
    writer.println("-help")
    assert(reader.readLine() == preString + "AAAAno command typed: use -help to get the valid commands list")


    Thread.sleep(1000) // Give thread time to process

    writer.close()
    pipeIn.close()
    pipeOut.close()