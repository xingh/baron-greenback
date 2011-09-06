package com.googlecode.barongreenback.jobs;

import com.googlecode.utterlyidle.Resources;
import com.googlecode.utterlyidle.modules.ApplicationScopedModule;
import com.googlecode.utterlyidle.modules.Module;
import com.googlecode.utterlyidle.modules.RequestScopedModule;
import com.googlecode.utterlyidle.modules.ResourcesModule;
import com.googlecode.yadic.Container;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.googlecode.utterlyidle.annotations.AnnotatedBindings.annotatedClass;

public class JobsModule implements ResourcesModule, ApplicationScopedModule, RequestScopedModule {
    public Module addResources(Resources resources) throws Exception {
        resources.add(annotatedClass(JobsResource.class));
        return this;
    }


    public Module addPerRequestObjects(Container container) throws Exception {
        return this;
    }

    public Module addPerApplicationObjects(Container container) throws Exception {
        container.addInstance(ScheduledExecutorService.class, Executors.newScheduledThreadPool(5));
        container.add(HttpScheduler.class);
        return this;
    }
}
