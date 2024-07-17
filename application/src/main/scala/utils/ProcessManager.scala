package utils

import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
import sys.process.*

sealed trait ProcessManager(instruction: String):

  def execute(): String =
    val os = System.getProperty("os.name").toLowerCase
    val command = if os.contains("win") then
      "cmd /c "+ instruction 
    else
      instruction
    val result = Try:
      val output = Process(command).!!
      output

    result match
      case Success(output) =>
        "Command executed successfully: " +output
      case Failure(exception) =>
        "Error executing command:" + {exception.getMessage}

class TestProcessManager extends ProcessManager("echo test99")

class CVLauncher(val args:List[String]) extends ProcessManager("../domain/src/build/app "+ args.toArray.mkString("Array(", ", ", ")"))

class ExitApp extends ProcessManager("/'^C'")



