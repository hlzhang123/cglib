/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
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
 */
package net.sf.cglib.beans;

import java.beans.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import net.sf.cglib.core.*;
import org.objectweb.asm.ClassVisitor;

abstract public class BeanMap implements Map {
    public static final int SWITCH_STYLE_TRIE = ComplexOps.SWITCH_STYLE_TRIE;
    public static final int SWITCH_STYLE_HASH = ComplexOps.SWITCH_STYLE_HASH;

    private static final BeanMapKey KEY_FACTORY =
      (BeanMapKey)KeyFactory.create(BeanMapKey.class);
    interface BeanMapKey {
        public Object newInstance(Class type, int switchStyle);
    }

    public static BeanMap create(Object bean) {
        Generator gen = new Generator();
        gen.setBean(bean);
        return gen.create();
    }

    public static class Generator extends AbstractClassGenerator {
        private static final Source SOURCE = new Source(BeanMap.class, true);

        private Object bean;
        private int switchStyle;
        
        public Generator() {
            super(SOURCE);
        }

        public void setBean(Object bean) {
            this.bean = bean;
        }

        public void setSwitchStyle(int switchStyle) {
            this.switchStyle = switchStyle;
        }

        protected ClassLoader getDefaultClassLoader() {
            return bean.getClass().getClassLoader();
        }

        public BeanMap create() {
            Object key = KEY_FACTORY.newInstance(bean.getClass(), switchStyle);
            return (BeanMap)super.create(key);
        }

        public void generateClass(ClassVisitor v) throws Exception {
            setNamePrefix(BeanMap.class.getName());
            new BeanMapEmitter(v, getClassName(), bean.getClass(), switchStyle);
        }

        protected Object firstInstance(Class type) {
            return ((BeanMap)ReflectUtils.newInstance(type)).newInstance(bean);
        }

        protected Object nextInstance(Object instance) {
            return ((BeanMap)instance).newInstance(bean);
        }
    }
    
    abstract protected BeanMap newInstance(Object bean);

    protected Object bean;

    protected BeanMap() {
    }

    protected BeanMap(Object bean) {
        setBean(bean);
    }

    public void setBean(Object bean) {
        this.bean = bean;
    }

    public Object getBean() {
        return bean;
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean containsKey(Object key) {
        return keySet().contains(key);
    }

    public boolean containsValue(Object value) {
        for (Iterator it = keySet().iterator(); it.hasNext();) {
            Object v = get(it.next());
            if (((value == null) && (v == null)) || value.equals(v))
                return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return keySet().size() == 0;
    }

    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    public void putAll(Map t) {
        for (Iterator it = t.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            put(key, t.get(key));
        }
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof Map)) {
            return false;
        }
        Map other = (Map)o;
        if (size() != other.size()) {
            return false;
        }
        for (Iterator it = keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            if (!other.containsKey(key)) {
                return false;
            }
            Object v1 = get(key);
            Object v2 = other.get(key);
            if (!((v1 == null) ? v2 == null : v1.equals(v2))) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int code = 0;
        for (Iterator it = keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            Object value = get(key);
            code += ((key == null) ? 0 : key.hashCode()) ^
                ((value == null) ? 0 : value.hashCode());
        }
        return code;
    }

    // TODO: optimize
    public Set entrySet() {
        return Collections.unmodifiableMap(new HashMap(this)).entrySet();
    }

    public Collection values() {
        Set keys = keySet();
        List values = new ArrayList(keys.size());
        for (Iterator it = keys.iterator(); it.hasNext();) {
            values.add(get(it.next()));
        }
        return Collections.unmodifiableCollection(values);
    }
}
