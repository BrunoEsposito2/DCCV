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

package parser

enum CLICommand(val command: String, val usage: String, val description: String):
  case Help extends CLICommand("-help", "-help", "Shows this help message")
  case Quit extends CLICommand("-q", "-q", "Exits the application")
  case HideStream extends CLICommand("-hideStream", "-hideStream", "Hides the current camera stream")
  case ShowStream extends CLICommand("-showStream", "-showStream", "Shows the current camera stream")
  case ShowCameras extends CLICommand("-showCameras", "-showCameras", "Lists all available cameras and their status")
  case Subscribe extends CLICommand("-subscribe", "-subscribe <camera name>", "Subscribes to the specified camera's stream")
  case Input extends CLICommand("-input", "-input <input>", "Sends input to the specified camera")
  case Config extends CLICommand("-config", "-config <camera name> <param1> <param2> ...", "Configures parameters for the specified camera")

  def helpText: String = f"$usage%-50s$description"