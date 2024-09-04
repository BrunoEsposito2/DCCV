package actor

import akka.actor.typed
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import message.{Config, Input, InputServiceFailure, InputServiceMsg, InputServiceSuccess, Message, OutputServiceMsg, SetOutputRef}
import utils.{ClientLauncher, Info}

import java.io.PrintWriter
import scala.collection.immutable.Queue

type Ref = ActorRef[OutputServiceMsg]
type ArgsList2 = Queue[String]

type Behavior = akka.actor.typed.Behavior[Message]
type Context = ActorContext[Message]

object CameraManager:
  def apply(port:Integer, outputRef:Option[Ref] = Option.empty): CameraManager = new CameraManager(Info(), Option.empty, Option.empty, port, outputRef)
  def apply(port:Integer, outputRef:Ref): CameraManager = new CameraManager(Info(), Option.empty, Option.empty, port, Option(outputRef))
  private def apply(info:Info, child:Option[ClientLauncher], childStdin: Option[PrintWriter], port:Integer, outputRef:Option[Ref]): CameraManager = new CameraManager(info, child, childStdin, port, outputRef)

private class CameraManager(info:Info, child:Option[ClientLauncher], childStdin:Option[PrintWriter], port:Integer, outputRef:Option[Ref]) extends ReachableActor(info):

  override def create(): Behavior =
    Behaviors.setup { context => 
      context.system.receptionist.tell(Receptionist.register(ServiceKey[InputServiceMsg]("inputs"), context.self))
      CameraManager(setActorInfo(context), child, childStdin, port, outputRef).behavior()
    }

  override def setActorInfo(ctx: Context): Info =
     outputRef.isEmpty match {
       case true => super.setActorInfo(ctx).setActorType("CameraManager")
       case _ => super.setActorInfo(ctx).setActorType("CameraManager").addRef(outputRef.get)
     }
     
  override def behavior(): Behavior =
    Behaviors.setup { context =>
      Behaviors.receiveMessagePartial(getReachableBehavior.orElse(getManagingBehavior))
    }
    
  private def getManagingBehavior: PartialFunction[Message, Behavior] =
    case Config(replyTo, args) =>
      //on receiving new cv configuration, stop previous child if present and become an updated version of the CameraManager actor
      if(childStdin.nonEmpty)
        childStdin.get.println("k")
        Thread.sleep(1000)
      if(outputRef.nonEmpty)
        val newChild = Option(ClientLauncher(port, args, info.self.asInstanceOf[ActorRef[InputServiceMsg]], outputRef.get))
        Thread(newChild.get).start()
        Thread.sleep(1000)
        val stdin = newChild.get.getChildProcessStdin
        replyTo ! InputServiceSuccess(info)
        CameraManager(info, newChild, stdin, port, outputRef).behavior()
      else
        replyTo ! InputServiceFailure("outputRef undefined")
        Behaviors.same

    case Input(replyTo, arg) =>
      (child.isEmpty, childStdin.isEmpty) match {
        case (false, false) =>
          childStdin.get.println(arg)
          replyTo ! InputServiceSuccess(info)
          if(arg=="k") CameraManager(info, Option.empty, Option.empty, port, outputRef).behavior() else Behaviors.same
        case (false, true) =>
          Thread.sleep(1000)
          val newChildStdin = child.get.getChildProcessStdin
          if(newChildStdin.isEmpty)
            replyTo ! InputServiceFailure("Child process stdin still undefined, please retry in a few moments.")
            Behaviors.same
          else
            childStdin.get.println(arg)
            replyTo ! InputServiceSuccess(info)
            CameraManager(info, child, newChildStdin, port, outputRef).behavior()
        case (_, _) =>
          replyTo ! InputServiceFailure("Child process undefined: operation aborted for potential unwanted side effects.")
          Behaviors.same
        }
      
    case SetOutputRef(ref) =>
      println("RECEIVED NEW OUTPUTREF")
      CameraManager(info.resetLinkedActors().addRef(ref), child, childStdin, port, Option(ref)).behavior()