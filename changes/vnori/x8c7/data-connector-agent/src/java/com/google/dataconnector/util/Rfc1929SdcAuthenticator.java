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

import net.sourceforge.jsocks.socks.ProxyMessage;
import net.sourceforge.jsocks.socks.server.ServerAuthenticator;
import net.sourceforge.jsocks.socks.server.ServerAuthenticatorNone;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * This class implements both authentication and Layer 4 IP rule enforcement for incoming SOCKS5 
 * connections.  It uses the RFC1929 specification however the user name is a JSON packet used a 
 * log vehicle, while the password is used a key that only allows access to the specified rule 
 * sets in the configuration.
 * 
 * this extends {@link ServerAuthenticatorNone} because all we really want from the superclass
 * is one single method {@link ServerAuthenticatorNone#selectSocks5Authentication}.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class Rfc1929SdcAuthenticator extends ServerAuthenticatorNone {

   private static final Logger LOG = Logger.getLogger(Rfc1929SdcAuthenticator.class);
   
   // RFC1929 Method ID
   static final int METHOD_ID = 2;
  
   private String passKey; // RFC1929 password used to lookup rule set in the map.
   private String serverMetaData; // Raw JSON string from server
   
   /** the following are defined in the superclass {@link ServerAuthenticatorNone}
    * but there are declared with new names because this class doesn't have access to 
    * in, out members of the superclass.
    */
   private InputStream inStream;
   private OutputStream outStream;
   
   /** injected dependency */
   private SdcKeysManager keyManager;
   
   /** 
    * a constructor solely for Guice injection use and to get {@link SdcKeysManager}
    * initialized.
    */
   @Inject
   public Rfc1929SdcAuthenticator(SdcKeysManager keyManager) {
     this.keyManager = keyManager;
   }

  /**
   * Checks the destination IP and port specified in the {@link ProxyMessage} against the allowed 
   * IP:Port pair for this connection.  If its valid allow the connection to continue.  Reject all
   * non SOCKS 5 requests.  We also log  any cloud provided data if we have any.
   * 
   * @param msg the JSOCKS msg object representing the SOCKS connection parameters.
   * @returns true if request is allowed or false if its prevented by ruleset.
   */
  @Override
  public boolean checkRequest(ProxyMessage msg) {

    // This shouldn't happen but we should check anyways.
    if (msg.version != 5) {
      return false;
    }

    try {
      JSONObject serverMetadataJson = new JSONObject(serverMetaData);
      String name = serverMetadataJson.getString("name");
      String resource = serverMetadataJson.getString("resource");
      String user = serverMetadataJson.getString("user");
      String appId = serverMetadataJson.getString("appId");
      LOG.info(msg.getConnectionId() + " Incoming connection for rule id:" +  name + 
          " for resource:" + resource +  " cloud-user:" + user + " reported-appId:" + appId);
    } catch (JSONException e) {
      LOG.info(msg.getConnectionId() + " Cloud did not report metadata (old cloud clients?)");
    }

    // Is this a valid "secret key"
    if (keyManager == null) {
      LOG.warn("SDC server never sent me keys during registration. reject the request.");
      return false;
    }
    boolean rslt = keyManager.checkKeyIpPort(passKey, msg.host, msg.port);
    if (!rslt) {
      LOG.info("No key found. Rejecting access to " + msg.host + ":" + msg.port);
    }
    return rslt;
  }
  
  /**
   * Reads the authentication data from the session and validates request
   * 
   * @param s connected socket from incoming SOCKS client.
   * @throws IOException if there are any socket communication issues.
   */
  @Override
  public ServerAuthenticator startSession(Socket s) throws IOException {
    this.inStream = s.getInputStream();
    this.outStream = s.getOutputStream();

    if(this.inStream.read() != 5) {
      LOG.debug("received non-version 5 Socks msg. ignoring it.");
      return null;
    }

    if(!selectSocks5Authentication(this.inStream, this.outStream, METHOD_ID) ||  
       !doUserPasswordAuthentication(s, this.inStream, this.outStream)) {
      return null;
    }
    return this;
  }

  /**
   * Helper function that actually performs the authentication.
   * 
   * @param s connected socket from SOCKS client.
   * @param in input stream of socket.
   * @param out output stream of socket.
   * @returns true if authorized false if not.
   * @throws IOException if there any socket communication issues.
   */
  private boolean doUserPasswordAuthentication(Socket s,
                                               InputStream in,
                                               OutputStream out) 
                                               throws IOException {
    int version = in.read();
    if(version != 1) return false;
    // This byte tells us how many bytes the username will be.  Max is 255.
    int ulen = in.read();
    if(ulen < 0) return false;
    byte[] user = new byte[ulen];
    in.read(user);
    // This byte tells us how many bytes the password will be.  Max is 255.
    int plen = in.read();
    if(plen < 0) return false;
    byte[] password = new byte[plen];
    in.read(password);

    // We use the RFC1929 Username to pass metadata to the client about this connection.
    serverMetaData = new String(user);
    passKey = new String(password);
    
    // Verify passKey exists.
    if (keyManager == null) {
      LOG.debug("SDC server hasn't sent the keys yet. reject the request.");
      return false;
    }
    if (keyManager.containsKey(passKey)) {
      // we have a passkey that matches, we will check the dest later
      out.write(new byte[]{1,0});
    } else {
      // failed auth, we have no passwords that match
      LOG.debug("the key " + passKey + " is not recognized.");
      out.write(new byte[]{1,1});
      return false;
    }
    return true;
  }

  @Override
  public InputStream getInputStream() {
    return inStream;
  }

  @Override
  public OutputStream getOutputStream() {
    return outStream;
  }
}