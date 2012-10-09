/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.prism;

import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

/**
 * @author semancik
 *
 */
public interface Objectable extends Containerable {
	
	public String getOid();
	
	public void setOid(String oid);
	
	public String getVersion();
	
	public void setVersion(String version);
	
	public PolyStringType getName();
	
	public void setName(PolyStringType name);
	
	public String getDescription();
	
	public void setDescription(String description);
	
	/**
	 * Returns short string representing identity of this object.
	 * It should container object type, OID and name. It should be presented
	 * in a form suitable for log and diagnostic messages (understandable for
	 * system administrator).
	 */
	public String toDebugName();
	
	/**
	 * Returns short string identification of object type. It should be in a form
	 * suitable for log messages. There is no requirement for the type name to be unique,
	 * but it rather has to be compact. E.g. short element names are preferred to long
	 * QNames or URIs.
	 * @return
	 */
	public String toDebugType();

    public PrismObject asPrismObject();
    
    public void setupContainer(PrismObject object);

//	public <O extends Objectable> PrismObject<O> asPrismObject();
//    
//    public <O extends Objectable> void setupContainer(PrismObject<O> object);
}
