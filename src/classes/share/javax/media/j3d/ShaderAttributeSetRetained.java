/*
 * $RCSfile$
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision$
 * $Date$
 * $State$
 */

package javax.media.j3d;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import javax.vecmath.*;

/**
 * The ShaderAttributeSet object provides uniform attributes to shader
 * programs.
 */

class ShaderAttributeSetRetained extends NodeComponentRetained {
    // A list of pre-defined bits to indicate which attribute
    // operation in this ShaderAttributeSet object is needed.
    static final int ATTRIBUTE_SET_PUT              = 0x01;

    static final int ATTRIBUTE_SET_REMOVE           = 0x02;

    static final int ATTRIBUTE_SET_CLEAR            = 0x04;

    private Map attrs = new HashMap();

    // Lock used for synchronization of live state
    Object liveStateLock = new Object();

    /**
     * Constructs an empty ShaderAttributeSetretained object. The attributes set
     * is initially empty.
     */
    ShaderAttributeSetRetained() {
    }

    //
    // Methods for dealing with the (name, value) pairs for explicit
    // attributes
    //

    /**
     * Adds the specified shader attribute to the attributes set.
     * The newly specified attribute replaces an attribute with the
     * same name, if one already exists in the attributes set.
     *
     * @param attr the shader attribute to be added to the set
     *
     */
    void put(ShaderAttribute attr) {
	synchronized(liveStateLock) {
	    // System.out.println("ShaderAttributeSetRetained : put()");
	    ShaderAttributeRetained sAttr = (ShaderAttributeRetained)attr.retained;
	    // System.out.println("attr is " + attr );
	    // System.out.println("attrName is " + sAttr.attrName + " attr.Retained is "+ sAttr );
	    assert(sAttr != null);
	    attrs.put(sAttr.attrName, sAttr);
	    
	    if (source.isLive()) {
		sAttr.setLive(inBackgroundGroup, refCount);
		sAttr.copyMirrorUsers(this);
                
		sendMessage(ATTRIBUTE_SET_PUT, sAttr.mirror);
	    }	    
	}
    }

    /**
     * Retrieves the shader attribute with the specified
     * <code>attrName</code> from the attributes set. If attrName does
     * not exist in the attributes set, null is returned.
     *
     * @param attrName the name of the shader attribute to be retrieved
     *
     * @exception CapabilityNotSetException if appropriate capability is 
     * not set and this object is part of live or compiled scene graph
     */
    ShaderAttribute get(String attrName) {
	return (ShaderAttribute)((ShaderAttributeRetained)attrs.get(attrName)).source;
    }

    /**
     * Removes the shader attribute with the specified
     * <code>attrName</code> from the attributes set. If attrName does
     * not exist in the attributes set then nothing happens.
     *
     * @param attrName the name of the shader attribute to be removed
     */
    void remove(String attrName) {
	synchronized(liveStateLock) {
	    ShaderAttributeRetained sAttr = (ShaderAttributeRetained)attrs.get(attrName);
	    attrs.remove(attrName);
	    if (source.isLive()) {
		sAttr.clearLive(refCount);
		sAttr.removeMirrorUsers(this);
		
		sendMessage(ATTRIBUTE_SET_REMOVE, attrName);
	    }
	}
    }

    /**
     * Removes the specified shader attribute from the attributes
     * set. If the attribute does not exist in the attributes set then
     * nothing happens. Note that this method will <i>not</i> remove a
     * shader object other than the one specified, even if it has the
     * same name as the specified attribute. Applications that wish to
     * remove an attribute by name should use
     * <code>removeAttribute(String)</code>.
     *
     * @param attr the shader attribute to be removed
     */
    void remove(ShaderAttribute attr) {
	synchronized(liveStateLock) {   
	    String attrName = attr.getAttributeName();
	    if (attrs.get(attrName) == attr) {
		attrs.remove(attrName);
		if (source.isLive()) {
		    ((ShaderAttributeRetained)attr.retained).clearLive(refCount);
		    ((ShaderAttributeRetained)attr.retained).removeMirrorUsers(this);
		    
		    sendMessage(ATTRIBUTE_SET_REMOVE, attrName);
		}
	    }
	}
    }

    /**
     * Removes all shader attributes from the attributes set. The
     * attributes set will be empty following this call.
     *
     */
    void clear() {
	synchronized(liveStateLock) {
	    attrs.clear();
	    if(source.isLive()) {
		ShaderAttributeRetained[] sAttrs = new ShaderAttributeRetained[attrs.size()];
		sAttrs = (ShaderAttributeRetained[])attrs.values().toArray(sAttrs);
		for (int i = 0; i < sAttrs.length; i++) {
		    sAttrs[i].clearLive(refCount);
		    sAttrs[i].removeMirrorUsers(this);
		}
		sendMessage(ATTRIBUTE_SET_CLEAR, null);
	    }
	}
    }

    /**
     * Returns a shallow copy of the attributes set.
     *
     * @return a shallow copy of the attributes set
     *
     */
    ShaderAttribute[] getAll() {

	ShaderAttributeRetained[] sAttrsRetained = new ShaderAttributeRetained[attrs.size()];
	ShaderAttribute[] sAttrs = new ShaderAttribute[sAttrsRetained.length];
	sAttrsRetained = (ShaderAttributeRetained[])attrs.values().toArray(sAttrsRetained);
	for(int i=0; i < sAttrsRetained.length; i++) {
	    sAttrs[i] = (ShaderAttribute) sAttrsRetained[i].source;
	}
	
	return sAttrs;
    }

    /**
     * Returns the number of elements in the attributes set.
     *
     * @return the number of elements in the attributes set
     *
     */
    int size() {
	return attrs.size();
    }


    void updateNative(Canvas3D cv, ShaderProgramRetained shaderProgram) {        
        shaderProgram.setShaderAttributes(cv, this);
    }

    Map getAttrs() {
        return attrs;
    }
    

    void setLive(boolean backgroundGroup, int refCount) {
	
	// System.out.println("ShaderAttributeSetRetained.setLive()");
	ShaderAttributeRetained[] sAttrsRetained = new ShaderAttributeRetained[attrs.size()];
	sAttrsRetained = (ShaderAttributeRetained[])attrs.values().toArray(sAttrsRetained);
	for(int i=0; i < sAttrsRetained.length; i++) {
	    sAttrsRetained[i].setLive(backgroundGroup, refCount);
	}
	
	super.doSetLive(backgroundGroup, refCount);
        super.markAsLive();
    }

    synchronized void addAMirrorUser(Shape3DRetained shape) {

	super.addAMirrorUser(shape);

	ShaderAttributeRetained[] sAttrsRetained = new ShaderAttributeRetained[attrs.size()];
	sAttrsRetained = (ShaderAttributeRetained[])attrs.values().toArray(sAttrsRetained);
	for(int i=0; i < sAttrsRetained.length; i++) {
	    sAttrsRetained[i].addAMirrorUser(shape); 
	}
    }

    synchronized void removeAMirrorUser(Shape3DRetained shape) {
	super.removeAMirrorUser(shape);

	ShaderAttributeRetained[] sAttrsRetained = new ShaderAttributeRetained[attrs.size()];
	sAttrsRetained = (ShaderAttributeRetained[])attrs.values().toArray(sAttrsRetained);
	for(int i=0; i < sAttrsRetained.length; i++) {
	    sAttrsRetained[i].removeAMirrorUser(shape);
	}
    }


    synchronized void removeMirrorUsers(NodeComponentRetained node) {
	super.removeMirrorUsers(node);

	ShaderAttributeRetained[] sAttrsRetained = new ShaderAttributeRetained[attrs.size()];
	sAttrsRetained = (ShaderAttributeRetained[])attrs.values().toArray(sAttrsRetained);
	for(int i=0; i < sAttrsRetained.length; i++) {
	    sAttrsRetained[i].removeMirrorUsers(node);
	}
    }

    synchronized void copyMirrorUsers(NodeComponentRetained node) {
	super.copyMirrorUsers(node);

	ShaderAttributeRetained[] sAttrsRetained = new ShaderAttributeRetained[attrs.size()];
	sAttrsRetained = (ShaderAttributeRetained[])attrs.values().toArray(sAttrsRetained);
	for(int i=0; i < sAttrsRetained.length; i++) {
	    sAttrsRetained[i].copyMirrorUsers(node); 
	}
    }

    void clearLive(int refCount) {
	// System.out.println("ShaderAttributeSetRetained.clearLive()");
	
	super.clearLive(refCount);
	
	ShaderAttributeRetained[] sAttrsRetained = new ShaderAttributeRetained[attrs.size()];
	sAttrsRetained = (ShaderAttributeRetained[])attrs.values().toArray(sAttrsRetained);
	for(int i=0; i < sAttrsRetained.length; i++) {
	    sAttrsRetained[i].clearLive(refCount);
	}
    }

    synchronized void createMirrorObject() {
	// System.out.println("ShaderAttributeSetRetained : createMirrorObject");
        // This method should only call by setLive().
	if (mirror == null) {
            ShaderAttributeSetRetained mirrorSAS = new ShaderAttributeSetRetained();
	    mirror = mirrorSAS;
	    mirror.source = source;

	}
	initMirrorObject();
    }
    
    void initMirrorObject() {

	ShaderAttributeRetained[] sAttrs = new ShaderAttributeRetained[attrs.size()];
	sAttrs = (ShaderAttributeRetained[])attrs.values().toArray(sAttrs);
	// Need to copy the mirror attrs 
	for (int i = 0; i < sAttrs.length; i++) {
	    ShaderAttributeRetained mirrorSA = (ShaderAttributeRetained) sAttrs[i].mirror;
	    assert(mirrorSA != null);
	    ((ShaderAttributeSetRetained)mirror).attrs.put(mirrorSA.attrName, mirrorSA);
	}
    }
    
     /**
     * Update the "component" field of the mirror object with the  given "value"
     */
    synchronized void updateMirrorObject(int component, Object value) {

	// System.out.println("ShaderAttributeSetRetained : updateMirrorObject");
        
	ShaderAttributeSetRetained mirrorSAS = (ShaderAttributeSetRetained)mirror;
	
	if ((component & ATTRIBUTE_SET_PUT) != 0) {
	    // System.out.println("     -- ATTRIBUTE_SET_PUT");
	    ShaderAttributeRetained mirrorSA = (ShaderAttributeRetained)value;
 	    assert(mirrorSA != null);
	    ((ShaderAttributeSetRetained)mirror).attrs.put(mirrorSA.attrName, mirrorSA);
	}
	else if((component & ATTRIBUTE_SET_REMOVE) != 0) {
	    // System.out.println("     -- ATTRIBUTE_SET_REMOVE");
	    ((ShaderAttributeSetRetained)mirror).attrs.remove((String)value);
	}
	else if((component & ATTRIBUTE_SET_CLEAR) != 0) {
	    // System.out.println("     -- ATTRIBUTE_SET_CLEAR");
	    ((ShaderAttributeSetRetained)mirror).attrs.clear();
	}
	else {
	    assert(false);
	}
    }   

    final void sendMessage(int attrMask, Object attr) {

	ArrayList univList = new ArrayList();
	ArrayList gaList = Shape3DRetained.getGeomAtomsList(mirror.users, univList);  

	// Send to rendering attribute structure, regardless of
	// whether there are users or not (alternate appearance case ..)
	J3dMessage createMessage = VirtualUniverse.mc.getMessage();
	createMessage.threads = J3dThread.UPDATE_RENDERING_ATTRIBUTES;
	createMessage.type = J3dMessage.SHADER_ATTRIBUTE_SET_CHANGED;
	createMessage.universe = null;
	createMessage.args[0] = this;
	createMessage.args[1]= new Integer(attrMask);
	createMessage.args[2] = attr;
	//	System.out.println("changedFreqent1 = "+changedFrequent);
	createMessage.args[3] = new Integer(changedFrequent);
	VirtualUniverse.mc.processMessage(createMessage);

	// System.out.println("univList.size is " + univList.size());
	for(int i=0; i<univList.size(); i++) {
	    createMessage = VirtualUniverse.mc.getMessage();
	    createMessage.threads = J3dThread.UPDATE_RENDER;
	    createMessage.type = J3dMessage.SHADER_ATTRIBUTE_SET_CHANGED;
		
	    createMessage.universe = (VirtualUniverse) univList.get(i);
	    createMessage.args[0] = this;
	    createMessage.args[1]= new Integer(attrMask);
	    createMessage.args[2] = attr;

	    ArrayList gL = (ArrayList)gaList.get(i);
	    GeometryAtom[] gaArr = new GeometryAtom[gL.size()];
	    gL.toArray(gaArr);
	    createMessage.args[3] = gaArr;  
	    
	    VirtualUniverse.mc.processMessage(createMessage);
	}

    }

}