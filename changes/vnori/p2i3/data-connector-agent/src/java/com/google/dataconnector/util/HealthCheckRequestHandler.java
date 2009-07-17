/* Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package com.google.dataconnector.util;

import com.google.inject.Inject;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class HealthCheckRequestHandler extends Thread {
  
  public static Logger LOG = Logger.getLogger(HealthCheckRequestHandler.class.getName());
  
  private ServerSocket serverSocket;
  private boolean quit = false;

  @Inject
  public HealthCheckRequestHandler(ServerSocket serverSocket) {
    this.serverSocket = serverSocket;
  }
  
  /**
   * Returns the port this service is listening on. This is used when system rules such as
   * healthcheck service rule are added to the user-defined list of resource rules.
   *  
   * @return the port the healthcheck service is listening on.
   */
  public int getPort() {
    if (serverSocket == null) {
      return -1;
    }
    return serverSocket.getLocalPort();
  }
  
  /**
   * Initializes the HealthCheckRequestHandler in a separate thread after opneing a ServerSocket
   */
  public void init() {
    
    // make this a daemon thread, so it quits when non-daemon threads exit.
    // TODO: make all daemon threads non-daemons and make them quit when they are told to,
    // for example by calling setQuitFlag() in this class to make this thread exit.
    setDaemon(true);
    
    // start the service in a separate thread
    start();
    LOG.info("healthcheck service started on port " + getPort());
  }
  
  /**
   * the run method of the HealthCheckRequestHandler thread. It waits to receive a socket connect 
   * request from the callers wishing to send "GET /healthcheck" http request. 
   * upon receiving the /healthcheck request, it responds with a simple "ok" response.
   */
  @Override
  public void run() {
    setName("HealthCheckRequestHandler");
    try {
      while (!quit) {
        Socket incomingSocket = serverSocket.accept();

        // read incoming request
        BufferedReader in = new BufferedReader(new InputStreamReader(
            incomingSocket.getInputStream()));
        PrintWriter out = new PrintWriter(incomingSocket.getOutputStream(), true);
        String inputStr;
        while ((inputStr = in.readLine()) != null) {
          if (inputStr.contains("/healthcheck")) {
            LOG.debug("healthcheck req found");
            break;
          }
        }
        
        // send http response
        out.print("HTTP/1.1 200 OK\r\n");
        out.print("Server: SDC_agent\r\n"); // TODO: include version# etc details
        out.print("Cache-Control: no-cache\r\n");
        out.print("Pragma: no-cache\r\n");
        out.print("Content-Type: text/plain\r\n");
        out.print("Content-Length: 2\r\n");
        out.print("\r\n");
        out.print("ok\r\n");
        out.flush();
        out.close();
        LOG.debug("processed healthcheck request");
      }
    } catch (IOException e) {
      LOG.warn("Healthcheck service IOException", e);
    }
  }

  /**
   * this method is called to let the thread know that it should quit.
   */
  public void setQuitFlag() {
    this.quit = true;
  }
}
