/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009-11, Red Hat Middleware LLC, and others contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.riftsaw.engine;

import static org.junit.Assert.*;

import javax.xml.namespace.QName;

import org.apache.ode.utils.DOMUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.riftsaw.engine.internal.BPELEngineImpl;
import org.w3c.dom.Element;

public class BPELEngineTest {

	private static BPELEngine m_engine=null;
	
	@BeforeClass
	public static void runBeforeClass() {
		m_engine = BPELEngineFactory.getEngine();
		
		try {
			m_engine.init();
		} catch(Exception e) {
			fail("Failed to initialize the engine: "+e);
		}
	}
	
	@AfterClass
	public static void runAfterClass() {
		try {
			m_engine.close();
		} catch(Exception e) {
			fail("Failed to close down the engine: "+e);
		}
	}
	
	public void deploy(String descriptor, String processName) throws Exception {
		java.net.URL url=BPELEngineImpl.class.getResource(descriptor);
		
		java.io.File deployFile=new java.io.File(url.getFile());
		
		DeploymentUnit bdu=new DeploymentUnit(processName, deployFile.lastModified(), deployFile);

		// Deploy the process
		m_engine.deploy(bdu);
	}
	
	public void undeploy(String descriptor, String processName) throws Exception {
		java.net.URL url=BPELEngineImpl.class.getResource(descriptor);
		
		java.io.File deployFile=new java.io.File(url.getFile());
		
		DeploymentUnit bdu=new DeploymentUnit(processName, deployFile.lastModified(), deployFile);

		// Deploy the process
		try {
			m_engine.undeploy(bdu);
		} catch(Throwable t) {
			// TODO: Ignore for now, until referential integrity issue resolved
		}
	}
	
	public void invoke(QName serviceName, String portName, String operation, String reqFile, String respFile,
								String faultName) throws Exception {
		java.net.URL url=BPELEngineImpl.class.getResource(reqFile);
		
		java.io.InputStream is=url.openStream();
		
		byte[] b=new byte[is.available()];
		is.read(b);
		
		is.close();
	
		org.w3c.dom.Element mesgElem=DOMUtils.stringToDOM(new String(b));
	
		// invoke ODE
		Element resp=null;

		try {
			resp  = m_engine.invoke(serviceName, portName, operation, mesgElem, null);
		} catch(Fault fault) {
			if (faultName == null) {
				fail("Unexpected fault '"+fault.getFaultName()+"' has occurred");
			} else if (faultName.equals(fault.getFaultName()) == false) {
				fail("Fault has occurred, but has different name. Expecting '"+
								faultName+"' but got '"+fault.getFaultName()+"'");
			} else {
				resp = fault.getFaultMessage();
			}
		}
			
		if (resp != null) {
			if (respFile == null) {
				fail("Response received but no file to verify it against");
			}
			
			String respText=DOMUtils.domToString(resp);
			
			url = BPELEngineImpl.class.getResource(respFile);
			
			is = url.openStream();
			
			b = new byte[is.available()];
			is.read(b);
			
			is.close();
			
			String respFileText=new String(b);
			
			if (respFileText.equals(respText) == false) {
				fail("Responses differ: file="+respFileText+" message="+respText);
			}
		}
	}
	
	@Test
	public void testHelloWorld() {
		
		try {
			deploy("/hello_world/deploy.xml", "hello_world");
			invoke(new QName("http://www.jboss.org/bpel/examples/wsdl","HelloService"), "HelloPort",
					"hello", "/hello_world/hello_request1.xml", "/hello_world/hello_response1.xml", null);
			undeploy("/hello_world/deploy.xml", "hello_world");
		} catch(Exception e) {
			fail("Failed: "+e);
		}
	}

}