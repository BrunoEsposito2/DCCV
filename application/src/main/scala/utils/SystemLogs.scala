package utils

enum InputServiceErrors(val log: String):
  case MissingChild extends InputServiceErrors("Child process undefined: operation aborted for potential unwanted side effects.")
  case MissingStdin extends InputServiceErrors("Child process stdin still undefined, please retry in a few moments.")
  case MissingConnection extends InputServiceErrors("The launched process was unable to connect to the socket")
  
enum StandardChildProcessCommands(val command: String):
  case Kill extends StandardChildProcessCommands("k")

  
  
