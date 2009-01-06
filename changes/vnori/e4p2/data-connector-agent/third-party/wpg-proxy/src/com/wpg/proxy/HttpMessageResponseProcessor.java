/*
 * Java HTTP Proxy Library (wpg-proxy), more info at
 * http://wpg-proxy.sourceforge.net/
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * 
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.wpg.proxy;

/** Process requests before they are sent or responses once they are received. */
public interface HttpMessageResponseProcessor {
  
  /** Continue to run next processors? */
  public boolean doContinue(HttpMessageResponse input);

  /** Send this message? */
  public boolean doSend(HttpMessageResponse input);

  /** Process this given message, returns the message after modification */
  public HttpMessageResponse process(HttpMessageResponse input);
}