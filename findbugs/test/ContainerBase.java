/*
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * [Additional notices, if required by prior licensing conditions]
 *
 */


package org.apache.catalina.core;


import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import javax.servlet.ServletException;
import javax.naming.directory.DirContext;
import javax.management.ObjectName;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;

import org.apache.naming.resources.ProxyDirContext;
import org.apache.catalina.Cluster;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Logger;
import org.apache.catalina.Manager;
import org.apache.catalina.Mapper;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;


/**
 * Abstract implementation of the <b>Container</b> interface, providing common
 * functionality required by nearly every implementation.  Classes extending
 * this base class must implement <code>getInfo()</code>, and may implement
 * a replacement for <code>invoke()</code>.
 * <p>
 * All subclasses of this abstract base class will include support for a
 * Pipeline object that defines the processing to be performed for each request
 * received by the <code>invoke()</code> method of this class, utilizing the
 * "Chain of Responsibility" design pattern.  A subclass should encapsulate its
 * own processing functionality as a <code>Valve</code>, and configure this
 * Valve into the pipeline by calling <code>setBasic()</code>.
 * <p>
 * This implementation fires property change events, per the JavaBeans design
 * pattern, for changes in singleton properties.  In addition, it fires the
 * following <code>ContainerEvent</code> events to listeners who register
 * themselves with <code>addContainerListener()</code>:
 * <table border=1>
 *   <tr>
 *     <th>Type</th>
 *     <th>Data</th>
 *     <th>Description</th>
 *   </tr>
 *   <tr>
 *     <td align=center><code>addChild</code></td>
 *     <td align=center><code>Container</code></td>
 *     <td>Child container added to this Container.</td>
 *   </tr>
 *   <tr>
 *     <td align=center><code>addValve</code></td>
 *     <td align=center><code>Valve</code></td>
 *     <td>Valve added to this Container.</td>
 *   </tr>
 *   <tr>
 *     <td align=center><code>removeChild</code></td>
 *     <td align=center><code>Container</code></td>
 *     <td>Child container removed from this Container.</td>
 *   </tr>
 *   <tr>
 *     <td align=center><code>removeValve</code></td>
 *     <td align=center><code>Valve</code></td>
 *     <td>Valve removed from this Container.</td>
 *   </tr>
 *   <tr>
 *     <td align=center><code>start</code></td>
 *     <td align=center><code>null</code></td>
 *     <td>Container was started.</td>
 *   </tr>
 *   <tr>
 *     <td align=center><code>stop</code></td>
 *     <td align=center><code>null</code></td>
 *     <td>Container was stopped.</td>
 *   </tr>
 * </table>
 * Subclasses that fire additional events should document them in the
 * class comments of the implementation class.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.1 $ $Date: 2003/03/27 22:45:21 $
 */

public abstract class ContainerBase
    implements Container, Lifecycle, Pipeline, MBeanRegistration {

    private static org.apache.commons.logging.Log log=
        org.apache.commons.logging.LogFactory.getLog( ContainerBase.class );

    /**
     * Perform addChild with the permissions of this class.
     * addChild can be called with the XML parser on the stack,
     * this allows the XML parser to have fewer privileges than
     * Tomcat.
     */
    protected class PrivilegedAddChild
        implements PrivilegedAction {

        private Container child;

        PrivilegedAddChild(Container child) {
            this.child = child;
        }

        public Object run() {
            addChildInternal(child);
            return null;
        }

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The child Containers belonging to this Container, keyed by name.
     */
    protected HashMap children = new HashMap();


    /**
     * The debugging detail level for this component.
     */
    protected int debug = 0;


    /**
     * The lifecycle event support for this component.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * The container event listeners for this Container.
     */
    protected ArrayList listeners = new ArrayList();


    /**
     * The Loader implementation with which this Container is associated.
     */
    protected Loader loader = null;


    /**
     * The Logger implementation with which this Container is associated.
     */
    protected Logger logger = null;


    /**
     * The Manager implementation with which this Container is associated.
     */
    protected Manager manager = null;


    /**
     * The cluster with which this Container is associated.
     */
    protected Cluster cluster = null;


    /**
     * The one and only Mapper associated with this Container, if any.
     */
    protected Mapper mapper = null;


    /**
     * The set of Mappers associated with this Container, keyed by protocol.
     */
    protected HashMap mappers = new HashMap();


    /**
     * The Java class name of the default Mapper class for this Container.
     */
    protected String mapperClass = null;


    /**
     * The human-readable name of this Container.
     */
    protected String name = null;


    /**
     * The parent Container to which this Container is a child.
     */
    protected Container parent = null;


    /**
     * The parent class loader to be configured when we install a Loader.
     */
    protected ClassLoader parentClassLoader = null;


    /**
     * The Pipeline object with which this Container is associated.
     */
    protected Pipeline pipeline = new StandardPipeline(this);


    /**
     * The Realm with which this Container is associated.
     */
    protected Realm realm = null;


    /**
     * The resources DirContext object with which this Container is associated.
     */
    protected DirContext resources = null;


    /**
     * The string manager for this package.
     */
    protected static StringManager sm =
        StringManager.getManager(Constants.Package);


    /**
     * Has this component been started?
     */
    protected boolean started = false;


    /**
     * The property change support for this component.
     */
    protected PropertyChangeSupport support = new PropertyChangeSupport(this);


    // ------------------------------------------------------------- Properties


    /**
     * Return the debugging detail level for this component.
     */
    public int getDebug() {

        return (this.debug);

    }


    /**
     * Set the debugging detail level for this component.
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {

        int oldDebug = this.debug;
        this.debug = debug;
        support.firePropertyChange("debug", new Integer(oldDebug),
                                   new Integer(this.debug));

    }


    /**
     * Return descriptive information about this Container implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public abstract String getInfo();


    /**
     * Return the Loader with which this Container is associated.  If there is
     * no associated Loader, return the Loader associated with our parent
     * Container (if any); otherwise, return <code>null</code>.
     */
    public Loader getLoader() {

        if (loader != null)
            return (loader);
        if (parent != null)
            return (parent.getLoader());
        return (null);

    }


    /**
     * Set the Loader with which this Container is associated.
     *
     * @param loader The newly associated loader
     */
    public synchronized void setLoader(Loader loader) {

        // Change components if necessary
        Loader oldLoader = this.loader;
        if (oldLoader == loader)
            return;
        this.loader = loader;

        // Stop the old component if necessary
        if (started && (oldLoader != null) &&
            (oldLoader instanceof Lifecycle)) {
            try {
                ((Lifecycle) oldLoader).stop();
            } catch (LifecycleException e) {
                log.error("ContainerBase.setLoader: stop: ", e);
            }
        }

        // Start the new component if necessary
        if (loader != null)
            loader.setContainer(this);
        if (started && (loader != null) &&
            (loader instanceof Lifecycle)) {
            try {
                ((Lifecycle) loader).start();
            } catch (LifecycleException e) {
                log.error("ContainerBase.setLoader: start: ", e);
            }
        }

        // Report this property change to interested listeners
        support.firePropertyChange("loader", oldLoader, this.loader);

    }


    /**
     * Return the Logger with which this Container is associated.  If there is
     * no associated Logger, return the Logger associated with our parent
     * Container (if any); otherwise return <code>null</code>.
     */
    public Logger getLogger() {

        if (logger != null)
            return (logger);
        if (parent != null)
            return (parent.getLogger());
        return (null);

    }


    /**
     * Set the Logger with which this Container is associated.
     *
     * @param logger The newly associated Logger
     */
    public synchronized void setLogger(Logger logger) {

        // Change components if necessary
        Logger oldLogger = this.logger;
        if (oldLogger == logger)
            return;
        this.logger = logger;

        // Stop the old component if necessary
        if (started && (oldLogger != null) &&
            (oldLogger instanceof Lifecycle)) {
            try {
                ((Lifecycle) oldLogger).stop();
            } catch (LifecycleException e) {
                log.error("ContainerBase.setLogger: stop: ", e);
            }
        }

        // Start the new component if necessary
        if (logger != null)
            logger.setContainer(this);
        if (started && (logger != null) &&
            (logger instanceof Lifecycle)) {
            try {
                ((Lifecycle) logger).start();
            } catch (LifecycleException e) {
                log.error("ContainerBase.setLogger: start: ", e);
            }
        }

        // Report this property change to interested listeners
        support.firePropertyChange("logger", oldLogger, this.logger);

    }


    /**
     * Return the Manager with which this Container is associated.  If there is
     * no associated Manager, return the Manager associated with our parent
     * Container (if any); otherwise return <code>null</code>.
     */
    public Manager getManager() {

        if (manager != null)
            return (manager);
        if (parent != null)
            return (parent.getManager());
        return (null);

    }


    /**
     * Set the Manager with which this Container is associated.
     *
     * @param manager The newly associated Manager
     */
    public synchronized void setManager(Manager manager) {

        // Change components if necessary
        Manager oldManager = this.manager;
        if (oldManager == manager)
            return;
        this.manager = manager;

        // Stop the old component if necessary
        if (started && (oldManager != null) &&
            (oldManager instanceof Lifecycle)) {
            try {
                ((Lifecycle) oldManager).stop();
            } catch (LifecycleException e) {
                log.error("ContainerBase.setManager: stop: ", e);
            }
        }

        // Start the new component if necessary
        if (manager != null)
            manager.setContainer(this);
        if (started && (manager != null) &&
            (manager instanceof Lifecycle)) {
            try {
                ((Lifecycle) manager).start();
            } catch (LifecycleException e) {
                log.error("ContainerBase.setManager: start: ", e);
            }
        }

        // Report this property change to interested listeners
        support.firePropertyChange("manager", oldManager, this.manager);

    }


    /**
     * Return an object which may be utilized for mapping to this component.
     */
    public Object getMappingObject() {
        return this;
    }


    /**
     * Return the Cluster with which this Container is associated.  If there is
     * no associated Cluster, return the Cluster associated with our parent
     * Container (if any); otherwise return <code>null</code>.
     */
    public Cluster getCluster() {
        if (cluster != null)
            return (cluster);

        if (parent != null)
            return (parent.getCluster());

        return (null);
    }


    /**
     * Set the Cluster with which this Container is associated.
     *
     * @param cluster The newly associated Cluster
     */
    public synchronized void setCluster(Cluster cluster) {
        // Change components if necessary
        Cluster oldCluster = this.cluster;
        if (oldCluster == cluster)
            return;
        this.cluster = cluster;

        // Stop the old component if necessary
        if (started && (oldCluster != null) &&
            (oldCluster instanceof Lifecycle)) {
            try {
                ((Lifecycle) oldCluster).stop();
            } catch (LifecycleException e) {
                log.error("ContainerBase.setCluster: stop: ", e);
            }
        }

        // Start the new component if necessary
        if (cluster != null)
            cluster.setContainer(this);

        if (started && (cluster != null) &&
            (cluster instanceof Lifecycle)) {
            try {
                ((Lifecycle) cluster).start();
            } catch (LifecycleException e) {
                log.error("ContainerBase.setCluster: start: ", e);
            }
        }

        // Report this property change to interested listeners
        support.firePropertyChange("cluster", oldCluster, this.cluster);
    }


    /**
     * Return a name string (suitable for use by humans) that describes this
     * Container.  Within the set of child containers belonging to a particular
     * parent, Container names must be unique.
     */
    public String getName() {

        return (name);

    }


    /**
     * Set a name string (suitable for use by humans) that describes this
     * Container.  Within the set of child containers belonging to a particular
     * parent, Container names must be unique.
     *
     * @param name New name of this container
     *
     * @exception IllegalStateException if this Container has already been
     *  added to the children of a parent Container (after which the name
     *  may not be changed)
     */
    public void setName(String name) {

        String oldName = this.name;
        this.name = name;
        support.firePropertyChange("name", oldName, this.name);
    }


    /**
     * Return the Container for which this Container is a child, if there is
     * one.  If there is no defined parent, return <code>null</code>.
     */
    public Container getParent() {

        return (parent);

    }


    /**
     * Set the parent Container to which this Container is being added as a
     * child.  This Container may refuse to become attached to the specified
     * Container by throwing an exception.
     *
     * @param container Container to which this Container is being added
     *  as a child
     *
     * @exception IllegalArgumentException if this Container refuses to become
     *  attached to the specified Container
     */
    public void setParent(Container container) {

        Container oldParent = this.parent;
        this.parent = container;
        support.firePropertyChange("parent", oldParent, this.parent);

    }


    /**
     * Return the parent class loader (if any) for this web application.
     * This call is meaningful only <strong>after</strong> a Loader has
     * been configured.
     */
    public ClassLoader getParentClassLoader() {
        if (parentClassLoader != null)
            return (parentClassLoader);
        if (parent != null) {
            return (parent.getParentClassLoader());
        }
        return (ClassLoader.getSystemClassLoader());

    }


    /**
     * Set the parent class loader (if any) for this web application.
     * This call is meaningful only <strong>before</strong> a Loader has
     * been configured, and the specified value (if non-null) should be
     * passed as an argument to the class loader constructor.
     *
     *
     * @param parent The new parent class loader
     */
    public void setParentClassLoader(ClassLoader parent) {
        ClassLoader oldParentClassLoader = this.parentClassLoader;
        this.parentClassLoader = parent;
        support.firePropertyChange("parentClassLoader", oldParentClassLoader,
                                   this.parentClassLoader);

    }


    /**
     * Return the Pipeline object that manages the Valves associated with
     * this Container.
     */
    public Pipeline getPipeline() {

        return (this.pipeline);

    }


    /**
     * Return the Realm with which this Container is associated.  If there is
     * no associated Realm, return the Realm associated with our parent
     * Container (if any); otherwise return <code>null</code>.
     */
    public Realm getRealm() {

        if (realm != null)
            return (realm);
        if (parent != null)
            return (parent.getRealm());
        return (null);

    }


    /**
     * Set the Realm with which this Container is associated.
     *
     * @param realm The newly associated Realm
     */
    public synchronized void setRealm(Realm realm) {

        // Change components if necessary
        Realm oldRealm = this.realm;
        if (oldRealm == realm)
            return;
        this.realm = realm;

        // Stop the old component if necessary
        if (started && (oldRealm != null) &&
            (oldRealm instanceof Lifecycle)) {
            try {
                ((Lifecycle) oldRealm).stop();
            } catch (LifecycleException e) {
                log.error("ContainerBase.setRealm: stop: ", e);
            }
        }

        // Start the new component if necessary
        if (realm != null)
            realm.setContainer(this);
        if (started && (realm != null) &&
            (realm instanceof Lifecycle)) {
            try {
                ((Lifecycle) realm).start();
            } catch (LifecycleException e) {
                log.error("ContainerBase.setRealm: start: ", e);
            }
        }

        // Report this property change to interested listeners
        support.firePropertyChange("realm", oldRealm, this.realm);

    }


    /**
      * Return the resources DirContext object with which this Container is
      * associated.  If there is no associated resources object, return the
      * resources associated with our parent Container (if any); otherwise
      * return <code>null</code>.
     */
    public DirContext getResources() {
        if (resources != null)
            return (resources);
        if (parent != null)
            return (parent.getResources());
        return (null);

    }


    /**
     * Set the resources DirContext object with which this Container is
     * associated.
     *
     * @param resources The newly associated DirContext
     */
    public synchronized void setResources(DirContext resources) {
        // Called from StandardContext.setResources()
        //              <- StandardContext.start() 
        //              <- ContainerBase.addChildInternal() 

        // Change components if necessary
        DirContext oldResources = this.resources;
        if (oldResources == resources)
            return;
        Hashtable env = new Hashtable();
        if (getParent() != null)
            env.put(ProxyDirContext.HOST, getParent().getName());
        env.put(ProxyDirContext.CONTEXT, getName());
        this.resources = new ProxyDirContext(env, resources);
        // Report this property change to interested listeners
        support.firePropertyChange("resources", oldResources, this.resources);

    }


    // ------------------------------------------------------ Container Methods


    /**
     * Add a new child Container to those associated with this Container,
     * if supported.  Prior to adding this Container to the set of children,
     * the child's <code>setParent()</code> method must be called, with this
     * Container as an argument.  This method may thrown an
     * <code>IllegalArgumentException</code> if this Container chooses not
     * to be attached to the specified Container, in which case it is not added
     *
     * @param child New child Container to be added
     *
     * @exception IllegalArgumentException if this exception is thrown by
     *  the <code>setParent()</code> method of the child Container
     * @exception IllegalArgumentException if the new child does not have
     *  a name unique from that of existing children of this Container
     * @exception IllegalStateException if this Container does not support
     *  child Containers
     */
    public void addChild(Container child) {
        if (System.getSecurityManager() != null) {
            PrivilegedAction dp =
                new PrivilegedAddChild(child);
            AccessController.doPrivileged(dp);
        } else {
            addChildInternal(child);
        }
    }

    private void addChildInternal(Container child) {

        synchronized(children) {
            if (children.get(child.getName()) != null)
                throw new IllegalArgumentException("addChild:  Child name '" +
                                                   child.getName() +
                                                   "' is not unique");
            child.setParent(this);  // May throw IAE
            if (started && (child instanceof Lifecycle)) {
                try {
                    ((Lifecycle) child).start();
                } catch (LifecycleException e) {
                    log.error("ContainerBase.addChild: start: ", e);
                    throw new IllegalStateException
                        ("ContainerBase.addChild: start: " + e);
                }
            }
            children.put(child.getName(), child);
            if( child instanceof ContainerBase ) {
                ContainerBase childCB=(ContainerBase)child;
                // XXX we should also send JMX notifications
                if( childCB.getObjectName() == null ) {
                    // child was not registered yet.
                    ObjectName oname=childCB.createObjectName(this.getDomain(),
                            this.getObjectName());
                    if( oname != null ) {
                        // XXX Register the child

                    }

                }
            }

            fireContainerEvent(ADD_CHILD_EVENT, child);
        }

    }


    /**
     * Add a container event listener to this component.
     *
     * @param listener The listener to add
     */
    public void addContainerListener(ContainerListener listener) {

        synchronized (listeners) {
            listeners.add(listener);
        }

    }


    /**
     * Add the specified Mapper associated with this Container.
     *
     * @param mapper The corresponding Mapper implementation
     *
     * @exception IllegalArgumentException if this exception is thrown by
     *  the <code>setContainer()</code> method of the Mapper
     */
    public void addMapper(Mapper mapper) {

        synchronized(mappers) {
            if (mappers.get(mapper.getProtocol()) != null)
                throw new IllegalArgumentException("addMapper:  Protocol '" +
                                                   mapper.getProtocol() +
                                                   "' is not unique");
            mapper.setContainer((Container) this);      // May throw IAE
            if (started && (mapper instanceof Lifecycle)) {
                try {
                    ((Lifecycle) mapper).start();
                } catch (LifecycleException e) {
                    log.error("ContainerBase.addMapper: start: ", e);
                    throw new IllegalStateException
                        ("ContainerBase.addMapper: start: " + e);
                }
            }
            mappers.put(mapper.getProtocol(), mapper);
            if (mappers.size() == 1)
                this.mapper = mapper;
            else
                this.mapper = null;
            fireContainerEvent(ADD_MAPPER_EVENT, mapper);
        }

    }


    /**
     * Add a property change listener to this component.
     *
     * @param listener The listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {

        support.addPropertyChangeListener(listener);

    }


    /**
     * Return the child Container, associated with this Container, with
     * the specified name (if any); otherwise, return <code>null</code>
     *
     * @param name Name of the child Container to be retrieved
     */
    public Container findChild(String name) {

        if (name == null)
            return (null);
        synchronized (children) {       // Required by post-start changes
            return ((Container) children.get(name));
        }

    }


    /**
     * Return the set of children Containers associated with this Container.
     * If this Container has no children, a zero-length array is returned.
     */
    public Container[] findChildren() {

        synchronized (children) {
            Container results[] = new Container[children.size()];
            return ((Container[]) children.values().toArray(results));
        }

    }


    /**
     * Return the set of container listeners associated with this Container.
     * If this Container has no registered container listeners, a zero-length
     * array is returned.
     */
    public ContainerListener[] findContainerListeners() {

        synchronized (listeners) {
            ContainerListener[] results = 
                new ContainerListener[listeners.size()];
            return ((ContainerListener[]) listeners.toArray(results));
        }

    }


    /**
     * Return the Mapper associated with the specified protocol, if there
     * is one.  If there is only one defined Mapper, use it for all protocols.
     * If there is no matching Mapper, return <code>null</code>.
     *
     * @param protocol Protocol for which to find a Mapper
     */
    public Mapper findMapper(String protocol) {

        if (mapper != null)
            return (mapper);
        else
            synchronized (mappers) {
                return ((Mapper) mappers.get(protocol));
            }

    }


    /**
     * Return the set of Mappers associated with this Container.  If this
     * Container has no Mappers, a zero-length array is returned.
     */
    public Mapper[] findMappers() {

        synchronized (mappers) {
            Mapper results[] = new Mapper[mappers.size()];
            return ((Mapper[]) mappers.values().toArray(results));
        }

    }


    /**
     * Process the specified Request, to produce the corresponding Response,
     * by invoking the first Valve in our pipeline (if any), or the basic
     * Valve otherwise.
     *
     * @param request Request to be processed
     * @param response Response to be produced
     *
     * @exception IllegalStateException if neither a pipeline or a basic
     *  Valve have been configured for this Container
     * @exception IOException if an input/output error occurred while
     *  processing
     * @exception ServletException if a ServletException was thrown
     *  while processing this request
     */
    public void invoke(Request request, Response response)
        throws IOException, ServletException {

        pipeline.invoke(request, response);

    }


    /**
     * Return the child Container that should be used to process this Request,
     * based upon its characteristics.  If no such child Container can be
     * identified, return <code>null</code> instead.
     *
     * @param request Request being processed
     * @param update Update the Request to reflect the mapping selection?
     */
    public Container map(Request request, boolean update) {

        // Select the Mapper we will use
        Mapper mapper = findMapper(request.getRequest().getProtocol());
        if (mapper == null)
            return (null);

        // Use this Mapper to perform this mapping
        return (mapper.map(request, update));

    }


    /**
     * Remove an existing child Container from association with this parent
     * Container.
     *
     * @param child Existing child Container to be removed
     */
    public void removeChild(Container child) {

        synchronized(children) {
            if (children.get(child.getName()) == null)
                return;
            children.remove(child.getName());
        }
        if (started && (child instanceof Lifecycle)) {
            try {
                if( child instanceof ContainerBase ) {
                    if( ((ContainerBase)child).started ) {
                        ((Lifecycle) child).stop();
                    }
                } else {
                    ((Lifecycle) child).stop();
                }
            } catch (LifecycleException e) {
                log.error("ContainerBase.removeChild: stop: ", e);
            }
        }
        if( child instanceof ContainerBase ) {
            ContainerBase childCB=(ContainerBase)child;
            // XXX we should also send JMX notifications
            ObjectName oname=childCB.getObjectName();
            if( oname != null ) {
                // XXX UnRegister the child
            }
        }
        fireContainerEvent(REMOVE_CHILD_EVENT, child);
        child.setParent(null);

    }


    /**
     * Remove a container event listener from this component.
     *
     * @param listener The listener to remove
     */
    public void removeContainerListener(ContainerListener listener) {

        synchronized (listeners) {
            listeners.remove(listener);
        }

    }


    /**
     * Remove a Mapper associated with this Container, if any.
     *
     * @param mapper The Mapper to be removed
     */
    public void removeMapper(Mapper mapper) {

        synchronized(mappers) {

            if (mappers.get(mapper.getProtocol()) == null)
                return;
            mappers.remove(mapper.getProtocol());
            if (started && (mapper instanceof Lifecycle)) {
                try {
                    ((Lifecycle) mapper).stop();
                } catch (LifecycleException e) {
                    log.error("ContainerBase.removeMapper: stop: ", e);
                    throw new IllegalStateException
                        ("ContainerBase.removeMapper: stop: " + e);
                }
            }
            if (mappers.size() != 1)
                this.mapper = null;
            else {
                Iterator values = mappers.values().iterator();
                this.mapper = (Mapper) values.next();
            }
            fireContainerEvent(REMOVE_MAPPER_EVENT, mapper);
        }

    }


    /**
     * Remove a property change listener from this component.
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {

        support.removePropertyChangeListener(listener);

    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * Add a lifecycle event listener to this component.
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {

        lifecycle.addLifecycleListener(listener);

    }


    /**
     * Get the lifecycle listeners associated with this lifecycle. If this 
     * Lifecycle has no listeners registered, a zero-length array is returned.
     */
    public LifecycleListener[] findLifecycleListeners() {

        return lifecycle.findLifecycleListeners();

    }


    /**
     * Remove a lifecycle event listener from this component.
     *
     * @param listener The listener to remove
     */
    public void removeLifecycleListener(LifecycleListener listener) {

        lifecycle.removeLifecycleListener(listener);

    }


    /**
     * Prepare for active use of the public methods of this Component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents it from being started
     */
    public synchronized void start() throws LifecycleException {

        // Validate and update our current component state
        if (started)
            throw new LifecycleException
                (sm.getString("containerBase.alreadyStarted", logName()));

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);

        addDefaultMapper(this.mapperClass);
        started = true;

        // Start our subordinate components, if any
        if ((loader != null) && (loader instanceof Lifecycle))
            ((Lifecycle) loader).start();
        if ((logger != null) && (logger instanceof Lifecycle))
            ((Lifecycle) logger).start();
        if ((manager != null) && (manager instanceof Lifecycle))
            ((Lifecycle) manager).start();
        if ((cluster != null) && (cluster instanceof Lifecycle))
            ((Lifecycle) cluster).start();
        if ((realm != null) && (realm instanceof Lifecycle))
            ((Lifecycle) realm).start();
        if ((resources != null) && (resources instanceof Lifecycle))
            ((Lifecycle) resources).start();

        // Start our Mappers, if any
        Mapper mappers[] = findMappers();
        for (int i = 0; i < mappers.length; i++) {
            if (mappers[i] instanceof Lifecycle)
                ((Lifecycle) mappers[i]).start();
        }

        // Start our child containers, if any
        Container children[] = findChildren();
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof Lifecycle)
                ((Lifecycle) children[i]).start();
        }

        // Start the Valves in our pipeline (including the basic), if any
        if (pipeline instanceof Lifecycle)
            ((Lifecycle) pipeline).start();

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(START_EVENT, null);

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);

    }


    /**
     * Gracefully shut down active use of the public methods of this Component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public synchronized void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started) {
            log.info(sm.getString("containerBase.notStarted", logName()));
            return;
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // Stop the Valves in our pipeline (including the basic), if any
        if (pipeline instanceof Lifecycle) {
            ((Lifecycle) pipeline).stop();
        }

        // Stop our child containers, if any
        Container children[] = findChildren();
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof Lifecycle)
                ((Lifecycle) children[i]).stop();
        }

        // Stop our Mappers, if any
        Mapper mappers[] = findMappers();
        for (int i = 0; i < mappers.length; i++) {
            if (mappers[(mappers.length-1)-i] instanceof Lifecycle)
                ((Lifecycle) mappers[(mappers.length-1)-i]).stop();
        }

        // Stop our subordinate components, if any
        if ((resources != null) && (resources instanceof Lifecycle)) {
            ((Lifecycle) resources).stop();
        }
        if ((realm != null) && (realm instanceof Lifecycle)) {
            ((Lifecycle) realm).stop();
        }
        if ((cluster != null) && (cluster instanceof Lifecycle)) {
            ((Lifecycle) cluster).stop();
        }
        if ((manager != null) && (manager instanceof Lifecycle)) {
            ((Lifecycle) manager).stop();
        }
        if ((logger != null) && (logger instanceof Lifecycle)) {
            ((Lifecycle) logger).stop();
        }
        if ((loader != null) && (loader instanceof Lifecycle)) {
            ((Lifecycle) loader).stop();
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);

    }


    // ------------------------------------------------------- Pipeline Methods


    /**
     * Add a new Valve to the end of the pipeline associated with this
     * Container.  Prior to adding the Valve, the Valve's
     * <code>setContainer</code> method must be called, with this Container
     * as an argument.  The method may throw an
     * <code>IllegalArgumentException</code> if this Valve chooses not to
     * be associated with this Container, or <code>IllegalStateException</code>
     * if it is already associated with a different Container.
     *
     * @param valve Valve to be added
     *
     * @exception IllegalArgumentException if this Container refused to
     *  accept the specified Valve
     * @exception IllegalArgumentException if the specifie Valve refuses to be
     *  associated with this Container
     * @exception IllegalStateException if the specified Valve is already
     *  associated with a different Container
     */
    public synchronized void addValve(Valve valve) {

        pipeline.addValve(valve);
        fireContainerEvent(ADD_VALVE_EVENT, valve);

    }


    /**
     * <p>Return the Valve instance that has been distinguished as the basic
     * Valve for this Pipeline (if any).
     */
    public Valve getBasic() {

        return (pipeline.getBasic());

    }


    /**
     * Return the set of Valves in the pipeline associated with this
     * Container, including the basic Valve (if any).  If there are no
     * such Valves, a zero-length array is returned.
     */
    public Valve[] getValves() {

        return (pipeline.getValves());

    }


    /**
     * Remove the specified Valve from the pipeline associated with this
     * Container, if it is found; otherwise, do nothing.
     *
     * @param valve Valve to be removed
     */
    public synchronized void removeValve(Valve valve) {

        pipeline.removeValve(valve);
        fireContainerEvent(REMOVE_VALVE_EVENT, valve);

    }


    /**
     * <p>Set the Valve instance that has been distinguished as the basic
     * Valve for this Pipeline (if any).  Prioer to setting the basic Valve,
     * the Valve's <code>setContainer()</code> will be called, if it
     * implements <code>Contained</code>, with the owning Container as an
     * argument.  The method may throw an <code>IllegalArgumentException</code>
     * if this Valve chooses not to be associated with this Container, or
     * <code>IllegalStateException</code> if it is already associated with
     * a different Container.</p>
     *
     * @param valve Valve to be distinguished as the basic Valve
     */
    public void setBasic(Valve valve) {

        pipeline.setBasic(valve);

    }



    // ------------------------------------------------------ Protected Methods


    /**
     * Add a default Mapper implementation if none have been configured
     * explicitly.
     *
     * @param mapperClass Java class name of the default Mapper
     */
    protected void addDefaultMapper(String mapperClass) {

        // Do we need a default Mapper?
        if (mapperClass == null)
            return;
        if (mappers.size() >= 1)
            return;

        // Instantiate and add a default Mapper
        try {
            Class clazz = Class.forName(mapperClass);
            Mapper mapper = (Mapper) clazz.newInstance();
            mapper.setProtocol("http");
            addMapper(mapper);
        } catch (Exception e) {
            log.error(sm.getString("containerBase.addDefaultMapper", mapperClass),
                e);
        }

    }


    /**
     * Notify all container event listeners that a particular event has
     * occurred for this Container.  The default implementation performs
     * this notification synchronously using the calling thread.
     *
     * @param type Event type
     * @param data Event data
     */
    public void fireContainerEvent(String type, Object data) {

        if (listeners.size() < 1)
            return;
        ContainerEvent event = new ContainerEvent(this, type, data);
        ContainerListener list[] = new ContainerListener[0];
        synchronized (listeners) {
            list = (ContainerListener[]) listeners.toArray(list);
        }
        for (int i = 0; i < list.length; i++)
            ((ContainerListener) list[i]).containerEvent(event);

    }


    /**
     * Log the specified message to our current Logger (if any).
     *
     * @param message Message to be logged
     */
    protected void log(String message) {

//         Logger logger = getLogger();
//         if (logger != null)
//             logger.log(logName() + ": " + message);
//         else
            log.info(message);
    }


    /**
     * Log the specified message and exception to our current Logger
     * (if any).
     *
     * @param message Message to be logged
     * @param throwable Related exception
     */
    protected void log(String message, Throwable throwable) {

        Logger logger = getLogger();
        if (logger != null)
            logger.log(logName() + ": " + message, throwable);
        else {
            log.error( message, throwable );
        }

    }


    /**
     * Return the abbreviated name of this container for logging messsages
     */
    protected String logName() {

        String className = this.getClass().getName();
        int period = className.lastIndexOf(".");
        if (period >= 0)
            className = className.substring(period + 1);
        return (className + "[" + getName() + "]");

    }

    // -------------------- JMX and Registration  --------------------
    protected String type;
    protected String domain;
    protected String suffix;
    protected ObjectName oname;
    protected MBeanServer mserver;

    public ObjectName getObjectName() {
        return oname;
    }

    public String getDomain() {
        return domain;
    }

    public String getType() {
        return type;
    }

    protected String getJSR77Suffix() {
        return suffix;
    }

    public ObjectName preRegister(MBeanServer server,
                                  ObjectName name) throws Exception {
        oname=name;
        mserver=server;
        domain=name.getDomain();

        type=name.getKeyProperty("type");
        if( type==null ) {
            type=name.getKeyProperty("j2eeType");
        }

        String j2eeApp=name.getKeyProperty("J2EEApplication");
        String j2eeServer=name.getKeyProperty("J2EEServer");
        if( j2eeApp==null ) {
            j2eeApp="none";
        }
        if( j2eeServer==null ) {
            j2eeServer="none";
        }
        suffix=",J2EEApplication=" + j2eeApp + ",J2EEServer=" + j2eeServer;
        return name;
    }

    public void postRegister(Boolean registrationDone) {
    }

    public void preDeregister() throws Exception {
    }

    public void postDeregister() {
    }

    public ObjectName[] getChildren() {
        ObjectName result[]=new ObjectName[children.size()];
        Iterator it=children.values().iterator();
        int i=0;
        while( it.hasNext() ) {
            Object next=it.next();
            if( next instanceof ContainerBase ) {
                result[i++]=((ContainerBase)next).getObjectName();
            }
        }
        return result;
    }

    public ObjectName createObjectName(String domain, ObjectName parent) {
        if( log.isDebugEnabled())
            log.debug("Create ObjectName " + domain + " " + parent );
        return null;
    }

}

