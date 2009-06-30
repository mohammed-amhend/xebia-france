/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.xebia.springframework.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.naming.SelfNaming;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author <a href="mailto:cyrille.leclerc@pobox.com">Cyrille Le Clerc</a>
 */
public class ThreadPoolExecutorFactory implements FactoryBean, InitializingBean, DisposableBean, BeanNameAware {
    
    private static class CountingRejectedExecutionHandler implements RejectedExecutionHandler {
        
        final private AtomicInteger rejectedExecutionCount = new AtomicInteger();
        
        private final RejectedExecutionHandler rejectedExecutionHandler;
        
        public CountingRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler) {
            super();
            this.rejectedExecutionHandler = rejectedExecutionHandler;
        }
        
        public int getRejectedExecutionCount() {
            return rejectedExecutionCount.get();
        }
        
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            rejectedExecutionCount.incrementAndGet();
            rejectedExecutionHandler.rejectedExecution(r, executor);
        }
        
    }
    
    @ManagedResource
    public static class SpringJmxEnabledThreadPoolExecutor extends ThreadPoolExecutor implements SelfNaming {
        
        private final ObjectName objectName;
        
        public SpringJmxEnabledThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                                  BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, ObjectName objectName) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory,
                  new CountingRejectedExecutionHandler(new AbortPolicy()));
            this.objectName = objectName;
        }
        
        @Override
        @ManagedAttribute(description = "Returns the approximate number of threads that are actively executing tasks")
        public int getActiveCount() {
            return super.getActiveCount();
        }
        
        @Override
        public ObjectName getObjectName() throws MalformedObjectNameException {
            return objectName;
        }
        
        @ManagedAttribute(description = "Returns the number of additional elements that this queue can "
                                        + "ideally (in the absence of memory or resource constraints) accept without  "
                                        + "blocking, or Integer.MAX_VALUE if there is no intrinsic limit.")
        public int getQueueRemainingCapacity() {
            return getQueue().remainingCapacity();
        }
        
        @ManagedAttribute(description = "Returns the number of tasks that has ever been rejected")
        public int getRejectedExecutionCount() {
            return ((CountingRejectedExecutionHandler)getRejectedExecutionHandler()).getRejectedExecutionCount();
        }
        
        @Override
        @ManagedAttribute(description = "Returns the approximate total number of tasks that have ever been scheduled for execution "
                                        + "(does not include the rejected tasks)")
        public long getTaskCount() {
            return super.getTaskCount();
        }
        
    }
    
    private String beanName;
    
    private int nbThreads;
    
    private String objectName;
    
    private int queueCapacity;
    
    private String threadNamePrefix;
    
    private ThreadPoolExecutor threadPoolExecutor;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        
        Assert.isTrue(this.nbThreads > 0, "nbThreads must be greater than zero");
        Assert.isTrue(this.queueCapacity > 0, "queueCapacity must be greater than zero");
        if (!StringUtils.hasLength(this.threadNamePrefix)) {
            this.threadNamePrefix = this.beanName + "-";
        }
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory(threadNamePrefix);
        threadFactory.setDaemon(true);
        
        if (!StringUtils.hasLength(objectName)) {
            objectName = "java.util.concurrent:type=ThreadPoolExecutor,name=" + ObjectName.quote(beanName);
        }
        this.threadPoolExecutor = new SpringJmxEnabledThreadPoolExecutor(nbThreads, nbThreads, 0L, TimeUnit.MILLISECONDS,
                                                                         new LinkedBlockingQueue<Runnable>(queueCapacity), threadFactory,
                                                                         new ObjectName(objectName));
    }
    
    @Override
    public void destroy() throws Exception {
        this.threadPoolExecutor.shutdown();
    }
    
    @Override
    public Object getObject() throws Exception {
        return threadPoolExecutor;
    }
    
    @Override
    public Class<?> getObjectType() {
        return this.threadPoolExecutor == null ? ThreadPoolExecutor.class : this.threadPoolExecutor.getClass();
    }
    
    @Override
    public boolean isSingleton() {
        return true;
    }
    
    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }
    
    public void setNbThreads(int nbThreads) {
        this.nbThreads = nbThreads;
    }
    
    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }
    
    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }
    
    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }
}
