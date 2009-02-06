/* Copyright 2008 Google Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.google.dataconnector.registration.v2;

import com.google.dataconnector.registration.v2.testing.FakeResourceRuleConfig;
import com.google.feedserver.util.BeanUtil;
import com.google.feedserver.util.XmlUtil;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.List;
import java.util.Random;

import javax.net.SocketFactory;

/**
 * Tests for the {@link ResourceRuleUtil} class.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ResoureRuleUtilTest extends TestCase {

  private static final int STARTING_HTTP_PROXY_PORT = 10000;
  
  private FakeResourceRuleConfig fakeResourceRuleConfig;
  ResourceRuleUtil resourceRuleUtil;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    resourceRuleUtil = new ResourceRuleUtil(new XmlUtil(), new BeanUtil(), null, null);
    fakeResourceRuleConfig = new FakeResourceRuleConfig();
  }
  
  @Override
  protected void tearDown() throws Exception {
    fakeResourceRuleConfig = null;
    resourceRuleUtil = null;
    super.tearDown();
  }
  
  public void testSetSecretKeys() {
    List<ResourceRule> resourceRules = fakeResourceRuleConfig.getFakeConfigResourceRules();
    resourceRuleUtil.setSecretKeys(resourceRules);
    for (ResourceRule resourceRule : resourceRules) {
      assertNotNull(resourceRule.getSecretKey());
    }
  }
  
  public void testSetHttpProxyPorts() {
    List<ResourceRule> resourceRules = fakeResourceRuleConfig.getFakeConfigResourceRules();
    resourceRuleUtil.setHttpProxyPorts(resourceRules, STARTING_HTTP_PROXY_PORT);
    int httpResourceCount = 0;
    for (ResourceRule resourceRule : resourceRules) {
      if (resourceRule.getPattern().startsWith(ResourceRule.HTTPID)) {
        assertEquals(STARTING_HTTP_PROXY_PORT + httpResourceCount, 
            resourceRule.getHttpProxyPort().intValue());
        httpResourceCount++;
      } else {
        assertNull(resourceRule.getHttpProxyPort());
      }
    }
  }
  
  public void testGetResourceRules() throws ResourceException {
    List<ResourceRule> resourceRules = resourceRuleUtil.getResourceRules(
        FakeResourceRuleConfig.RUNTIME_RESOURCE_RULES_XML);
    
    // Check Http Runtime - rule 0
    ResourceRule expected = fakeResourceRuleConfig.getRuntimeHttpResourceRule();
    ResourceRule actual = resourceRules.get(0);
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getPattern(), actual.getPattern());
    assertEquals(expected.getHttpProxyPort(), actual.getHttpProxyPort());
    verifyCommonResourceParams(expected, actual);
    
    // Socket Runtime - rule 1
    expected = fakeResourceRuleConfig.getRuntimeSocketResourceRule();
    actual = resourceRules.get(1);
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getPattern(), actual.getPattern());
    verifyCommonResourceParams(expected, actual);
  }
  
  public void testGetResourceRuleFromEntityXml() throws ResourceException {
    ResourceRule actual = resourceRuleUtil.getResourceRuleFromEntityXml(
        FakeResourceRuleConfig.RUNTIME_RESOURCE_ENTITY_XML);
    ResourceRule expected = fakeResourceRuleConfig.getRuntimeHttpResourceRule();
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getPattern(), actual.getPattern());
    assertEquals(expected.getHttpProxyPort(), actual.getHttpProxyPort());
    verifyCommonResourceParams(expected, actual);
  }
  
  public void testGetEntityXmlFromResourceRule() throws ResourceException {
    // For this test, we assume that getEntityXmlFromResourceRule works as its tested above. We
    // convert from a known good ResourceRule to XML then back again.  if they are equal this code
    // probably works :)
    ResourceRule actual = resourceRuleUtil.getResourceRuleFromEntityXml(
        resourceRuleUtil.getEntityXmlFromResourceRule(
            fakeResourceRuleConfig.getRuntimeHttpResourceRule()));
    ResourceRule expected = fakeResourceRuleConfig.getRuntimeHttpResourceRule();
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getPattern(), actual.getPattern());
    assertEquals(expected.getHttpProxyPort(), actual.getHttpProxyPort());
    verifyCommonResourceParams(expected, actual);
  }
  
  public void testGetVirtualHostBindPorts() throws ResourceException, IOException {
    
    // Setup
    SocketAddress mockSocketAddress = EasyMock.createMock(SocketAddress.class);
    EasyMock.replay(mockSocketAddress);
    
    Socket mockSocket = EasyMock.createMock(Socket.class);
    mockSocket.bind(EasyMock.isA(SocketAddress.class));
    EasyMock.expectLastCall().anyTimes();
    EasyMock.expect(mockSocket.getLocalPort()).andReturn(new Random().nextInt());
    mockSocket.close();
    EasyMock.expectLastCall();
    EasyMock.replay(mockSocket);
    
    SocketFactory mockSocketFactory = EasyMock.createMock(SocketFactory.class);
    EasyMock.expect(mockSocketFactory.createSocket()).andReturn(mockSocket).anyTimes();
    EasyMock.replay(mockSocketFactory);
    
    ResourceRuleUtil resourceRuleUtil = new ResourceRuleUtil(new XmlUtil(), new BeanUtil(),
        mockSocketFactory, mockSocketAddress);
    
    // Set the fake http proxy port to null.
    for (ResourceRule resourceRule : fakeResourceRuleConfig.getFakeRuntimeResourceRules()) {
      resourceRule.setHttpProxyPort(null);
    }
    
    // Test
    resourceRuleUtil.getVirtualHostBindPortsAndSetHttpProxyPorts(fakeResourceRuleConfig.getFakeRuntimeResourceRules());
    
    // Verify.  HTTP should have an integer, others should still have null.
    for (ResourceRule resourceRule : fakeResourceRuleConfig.getFakeRuntimeResourceRules()) {
      if (resourceRule.getPattern().startsWith(ResourceRule.HTTPID)) {
        assertNotNull(resourceRule.getHttpProxyPort());
      } else {
        assertNull(resourceRule.getHttpProxyPort()); 
      }
    }
    EasyMock.verify(mockSocket);
    EasyMock.verify(mockSocketFactory);
  }
  
  /**
   * Looks at common parameters between two resource rules.  HTTP and Socket rules always share
   * these parameters.
   * 
   * @param expected ResourceRule
   * @param actual ResourceRule
   */
  private void verifyCommonResourceParams(ResourceRule expected, ResourceRule actual) {
    assertEquals(expected.getClientId(), actual.getClientId());
    for (int index=0 ; index < expected.getAllowedEntities().length; index++) {
	  assertEquals(expected.getAllowedEntities()[index], actual.getAllowedEntities()[index]);
    }
    if (expected.getAppIds() != null) {
	  for (int index=0 ; index < expected.getAppIds().length; index++) {
		assertEquals(expected.getAppIds()[index], actual.getAppIds()[index]);
	  }
    }
    assertEquals(expected.getSocksServerPort(), actual.getSocksServerPort());
    assertEquals(expected.getSecretKey(), actual.getSecretKey());
  }
}