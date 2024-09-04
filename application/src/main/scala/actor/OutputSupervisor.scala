package actor

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, Behavior, receptionist}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import message.{GetInputs, GetOutputs, InputServiceMsg, InputsListing, Message, OutputListing, OutputServiceMsg, Ping, PingServiceMsg, Pong, SetOutputRef}

import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.util.Random

object OutputSupervisor:
  def apply(activeOutputsList: Set[ActorRef[OutputServiceMsg]] = Set()): OutputSupervisor = new OutputSupervisor(activeOutputsList)

private class OutputSupervisor(activeOutputsList: Set[ActorRef[OutputServiceMsg]]) extends Actor:

  def create(): Behavior[Message] =  
    Behaviors.withTimers { (timers: TimerScheduler[Message]) =>
      Behaviors.setup { ctx =>
        timers.startTimerWithFixedDelay("inputsTimer", GetInputs(), FiniteDuration(10, SECONDS))
        timers.startTimerWithFixedDelay("outputsTimer", GetOutputs(), FiniteDuration(5, SECONDS))
        val inputsKey = ServiceKey[InputServiceMsg]("inputs")
        val outputsKey = ServiceKey[OutputServiceMsg]("outputs")
        val listingResponseAdapter =
          ctx.messageAdapter[Receptionist.Listing] {
            case inputsKey.Listing(listings) => InputsListing(listings)
            case outputsKey.Listing(listings) => OutputListing(listings)
          }
        this.behavior(ctx, listingResponseAdapter)
      }
    }
  
  private def getSupervisorBehavior(ctx: ActorContext[Message], adapter: ActorRef[Receptionist.Listing]): PartialFunction[Message, Behavior[Message]] =
    case InputsListing(list) =>
      println(list.size.toString + " INPUTS RECEIVED")
      list.foreach(actor => actor.asInstanceOf[ActorRef[PingServiceMsg]] ! Ping(ctx.self))
      Behaviors.same

    case Pong(info) =>
      val outputRefOfCM: Option[ActorRef[OutputServiceMsg]] = 
        if (info.linkedActors.isEmpty) Option.empty 
        else Option(info.linkedActors.head.asInstanceOf[ActorRef[OutputServiceMsg]])
        
      if((outputRefOfCM.isEmpty || !activeOutputsList.contains(outputRefOfCM.get)) && activeOutputsList.nonEmpty) {
        if(outputRefOfCM.isEmpty) println("EMPTY")
        else if(!activeOutputsList.contains(outputRefOfCM.get)) println(outputRefOfCM.get.toString + " NOT IN LIST " + activeOutputsList)
        println("SENDING NEW OUTPUT TO AN ORPHANED INPUT")
        info.self.asInstanceOf[ActorRef[InputServiceMsg]] ! SetOutputRef(activeOutputsList.toSeq(Random.nextInt(activeOutputsList.size)))
      }
      Behaviors.same

    case GetInputs() =>
      println("GETTING INPUTS")
      ctx.system.receptionist ! Receptionist.find(ServiceKey[InputServiceMsg]("inputs"), adapter)
      Behaviors.same

    case GetOutputs() =>
      println("GETTING OUTPUTS")
      ctx.system.receptionist ! Receptionist.find(ServiceKey[OutputServiceMsg]("outputs"), adapter)
      Behaviors.same

    case OutputListing(set) =>
      OutputSupervisor(set).behavior(ctx, adapter)

  private def behavior(ctx: ActorContext[Message], adapter: ActorRef[Receptionist.Listing]): Behavior[Message] =
    Behaviors.setup { ctx =>
      Behaviors.receiveMessagePartial(getSupervisorBehavior(ctx, adapter))
    }



