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

import akka.actor.typed
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.stream.Materializer
import message.{ChildStatus, Config, ConfigServiceSuccess, GetChildStatus, Input, InputServiceFailure, InputServiceSuccess, Message, OutputServiceMsg, Subscribe, SubscribeServiceFailure, SubscribeServiceSuccess, Unsubscribe}
import utils.InputServiceErrors.MissingChild
import utils.{ActorTypes, ChildStatuses, ConnectionController, Info, InputServiceErrors, StandardChildProcessCommands, StreamController}

import java.io.{OutputStreamWriter, PrintWriter}
import scala.sys.process.*
import java.net.SocketTimeoutException
import scala.collection.immutable.Queue

type Ref = ActorRef[OutputServiceMsg]
type ArgsList2 = Queue[String]

type Behavior = akka.actor.typed.Behavior[Message]
type Context = ActorContext[Message]

object CameraManager:
  def apply(port:Integer): CameraManager = new CameraManager(Info(), Option.empty,  StreamController(), ConnectionController(port))
  private def apply(info:Info,
                    childStdin:Option[PrintWriter] = Option.empty,
                    childOutputStream: StreamController = StreamController(),
                    socket: ConnectionController): CameraManager = new CameraManager(info, childStdin, childOutputStream, socket)

private class CameraManager(info:Info, childStdin:Option[PrintWriter], childOutputStream: StreamController, socket:ConnectionController) extends ReachableActor:

  def create(): Behavior =
    Behaviors.setup { context =>
      context.system.receptionist.tell(Receptionist.register(ServiceKey[Message]("inputs"), context.self))
      implicit val ctx: ActorContext[Message] = context
      implicit val mat: Materializer = Materializer(ctx.system)
      CameraManager(setActorInfo(Info()), childStdin, childOutputStream, socket).behavior
    }

  override def setActorInfo(info:Info)(implicit ctx: Context): Info =
    super.setActorInfo(info).setActorType(ActorTypes.CameraManager)
     
  def behavior(implicit materializer: Materializer, ctx:ActorContext[Message]): Behavior =
    Behaviors.setup { ctx =>
      Behaviors.receiveMessagePartial(getReachableBehavior(info).orElse(getManagingBehavior))
    }

  private def launchNewChildProcess(command: Queue[String])(implicit materializer: Materializer): (PrintWriter, ConnectionController) =
    val bashScript = command.foldLeft("")((acc, arg) => if (acc.isEmpty) arg else acc + " " + arg)
    var processStdin: Option[PrintWriter] = Option.empty
    val processIO = ProcessIO(stdin => processStdin = Option(PrintWriter(OutputStreamWriter(stdin), true)), stdout => {}, stderr => {})
    //launch the bash script and wait for the launched program to reach the socket
    Process(bashScript).run(processIO)
    val newConnection = socket.enstablishConnection()
    (processStdin.get, newConnection)

  private def getManagingBehavior(implicit materializer: Materializer, ctx:ActorContext[Message]): PartialFunction[Message, Behavior] =
    case Config(replyTo, args) =>
      //on receiving new cv configuration, stop previous child if present and become an updated version of the CameraManager actor
      if(childStdin.nonEmpty) childStdin.get.println(StandardChildProcessCommands.Kill.command)
      val newSource = childOutputStream.closeSource()
      try
        val (newStdin, newConnection) = launchNewChildProcess(args)
        val ns = newSource.InitializeSource(newConnection.getClientInput.get, ctx.self)
        replyTo ! ConfigServiceSuccess(info)
        CameraManager(info, Option(newStdin), ns, newConnection).behavior
      catch
        case e: SocketTimeoutException =>
          replyTo ! InputServiceFailure(InputServiceErrors.MissingConnection)
          CameraManager(info, childStdin, childOutputStream, socket).behavior

    case Input(replyTo, arg) =>
      (childOutputStream.isStreamRunning, childStdin, arg) match
        case (false, _, _) =>
          replyTo ! InputServiceFailure(InputServiceErrors.MissingChild)
          CameraManager(info, childStdin, childOutputStream, socket).behavior
        case (true, None, _) =>
          replyTo ! InputServiceFailure(InputServiceErrors.MissingStdin)
          CameraManager(info, childStdin, childOutputStream, socket).behavior
        case (true, Some(inputStream), StandardChildProcessCommands.Kill.command) =>
          inputStream.println(arg)
          replyTo ! InputServiceSuccess(info)
          CameraManager(info, Option.empty, childOutputStream.closeSource(), socket.closeConnection()).behavior
        case (true, Some(inputStream), _) =>
          inputStream.println(arg)
          replyTo ! InputServiceSuccess(info)
          CameraManager(info, childStdin, childOutputStream, socket).behavior
      
    case Subscribe(replyTo, sinkRef) =>
      if childOutputStream.isStreamRunning then
        replyTo ! SubscribeServiceSuccess(info)
        CameraManager(info, childStdin, childOutputStream.addSink(replyTo, sinkRef), socket).behavior
      else
        replyTo ! SubscribeServiceFailure(info, MissingChild)
        CameraManager(info, childStdin, childOutputStream, socket).behavior
        
    case Unsubscribe(replyTo) =>
      CameraManager(info, childStdin, childOutputStream.removeSink(replyTo), socket).behavior
      
    case GetChildStatus(replyTo) =>
      (childOutputStream.isStreamRunning, childStdin) match
        case (true, Some(printWriter)) =>
          replyTo ! ChildStatus(info, ChildStatuses.Running)
        case _ => 
          replyTo ! ChildStatus(info, ChildStatuses.Idle)
      CameraManager(info, childStdin, childOutputStream, socket).behavior    
      
          