package com.googlecode.barongreenback.persistence;

import com.googlecode.barongreenback.persistence.lucene.LuceneModule;
import com.googlecode.barongreenback.persistence.sql.SqlModule;
import com.googlecode.barongreenback.shared.BaronGreenbackRequestScope;
import com.googlecode.barongreenback.shared.BaronGreenbackRequestScopedModule;
import com.googlecode.funclate.Model;
import com.googlecode.lazyrecords.IgnoreLogger;
import com.googlecode.lazyrecords.Logger;
import com.googlecode.lazyrecords.lucene.Persistence;
import com.googlecode.lazyrecords.mappings.StringMappings;
import com.googlecode.utterlyidle.Application;
import com.googlecode.utterlyidle.modules.ApplicationScopedModule;
import com.googlecode.utterlyidle.modules.Module;
import com.googlecode.utterlyidle.modules.RequestScopedModule;
import com.googlecode.yadic.Container;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.googlecode.yadic.Containers.addActivatorIfAbsent;
import static com.googlecode.yadic.Containers.addIfAbsent;
import static com.googlecode.yadic.Containers.addInstanceIfAbsent;

public class PersistenceModule implements ApplicationScopedModule, RequestScopedModule, BaronGreenbackRequestScopedModule {

    public Container addPerRequestObjects(final Container container) throws Exception {
        addIfAbsent(container, Logger.class, IgnoreLogger.class);
        addActivatorIfAbsent(container, Persistence.class, PersistenceActivator.class);
        addActivatorIfAbsent(container, BaronGreenbackRecords.class, BaronGreenbackRecordsActivator.class);
        addActivatorIfAbsent(container, BaronGreenbackStringMappings.class, BaronGreenbackStringMappingsActivator.class);
        return container;
    }

    @Override
    public Container addPerApplicationObjects(Container container) throws Exception {
        addIfAbsent(container, PersistenceUri.class);
        addIfAbsent(container, PersistenceUser.class);
        addIfAbsent(container, PersistencePassword.class);
        return container;
    }

    // TODO: Make reflective so we don't need lucene deps
    public static final String JDBC = "jdbc";
    public static final String LUCENE = "lucene";

    public static final Map<String, Module> modules = new ConcurrentHashMap<String, Module>() {{
        put(LUCENE, new LuceneModule());
        put(JDBC, new SqlModule());
    }};

    public static Application configure(Application application) {
        application.add(new PersistenceModule());
        PersistenceUri persistenceUri = application.applicationScope().get(PersistenceUri.class);
        application.add(modules.get(persistenceUri.scheme()));
        return application;
    }

    @Override
    public BaronGreenbackRequestScope addBaronGreenbackPerRequestObjects(BaronGreenbackRequestScope bgbRequestScope) {
        Container container = bgbRequestScope.value();
        addInstanceIfAbsent(container, StringMappings.class, new StringMappings().add(Model.class, new ModelMapping()));
        return bgbRequestScope;
    }
}
