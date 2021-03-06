/*
 * Copyright 2017-2018 Baidu Inc.
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

package com.baidu.openrasp.transformer;

import com.baidu.openrasp.ModuleLoader;
import com.baidu.openrasp.config.Config;
import com.baidu.openrasp.hook.*;
import com.baidu.openrasp.tool.annotation.AnnotationScanner;
import com.baidu.openrasp.tool.annotation.HookAnnotation;
import javassist.*;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.*;

/**
 * 自定义类字节码转换器，用于hook类德 方法
 */
public class CustomClassTransformer implements ClassFileTransformer {
    public static final Logger LOGGER = Logger.getLogger(CustomClassTransformer.class.getName());
    private static final String SCAN_ANNOTATION_PACKAGE = "com.baidu.openrasp.hook";
    private static ArrayList<String> list = new ArrayList<String>();
    private static HashMap<String, ClassLoader> classLoaderCache = new HashMap<String, ClassLoader>();

    private HashSet<AbstractClassHook> hooks;

    static {
        list.add("org/apache/catalina/util/ServerInfo");
        list.add("org/apache/commons/fileupload/servlet/ServletFileUpload");
    }

    public CustomClassTransformer() {
        hooks = new HashSet<AbstractClassHook>();
        addAnnotationHook();
    }

    private void addHook(AbstractClassHook hook, String className) {
        String[] ignore = Config.getConfig().getIgnoreHooks();
        for (String s : ignore) {
            if (hook.couldIgnore() && (s.equals("all") || s.equals(hook.getType()))) {
                LOGGER.info("ignore hook type " + hook.getType() + ", class " + className);
                return;
            }
        }
        hooks.add(hook);
    }

    private void addAnnotationHook() {
        Set<Class> classesSet = AnnotationScanner.getClassWithAnnotation(SCAN_ANNOTATION_PACKAGE, HookAnnotation.class);
        for (Class clazz : classesSet) {
            try {
                Object object = clazz.newInstance();
                if (object instanceof AbstractClassHook) {
                    addHook((AbstractClassHook) object, clazz.getName());
                }
            } catch (Exception e) {
                LOGGER.error("add hook failed", e);
            }
        }
    }

    /**
     * 过滤需要hook的类，进行字节码更改
     *
     * @see ClassFileTransformer#transform(ClassLoader, String, Class, ProtectionDomain, byte[])
     */
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain domain, byte[] classfileBuffer) throws IllegalClassFormatException {
        for (final AbstractClassHook hook : hooks) {
            if (hook.isClassMatched(className)) {
                CtClass ctClass = null;
                try {
                    ClassPool classPool = new ClassPool();
                    addLoader(classPool, loader);
                    ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                    if (loader == null) {
                        hook.setLoadedByBootstrapLoader(true);
                    }
                    classfileBuffer = hook.transformClass(ctClass);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (ctClass != null) {
                        ctClass.detach();
                    }
                }
            }
        }
        handleClassLoader(loader, className);
        return classfileBuffer;
    }

    private void addLoader(ClassPool classPool, ClassLoader loader) {
        classPool.appendSystemPath();
        classPool.appendClassPath(new ClassClassPath(ModuleLoader.class));
        if (loader != null) {
            classPool.appendClassPath(new LoaderClassPath(loader));
        }
    }

    public static ClassLoader getClassLoader(String className) {
        return classLoaderCache.get(className);
    }

    private static void handleClassLoader(ClassLoader loader, String className) {

        if (list.contains(className)) {
            classLoaderCache.put(className.replace('/', '.'), loader);
        }
    }

    public HashSet<AbstractClassHook> getHooks() {
        return hooks;
    }

}
