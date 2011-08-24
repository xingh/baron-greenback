package com.googlecode.barongreenback.crawler;

import com.googlecode.barongreenback.search.SearchResource;
import com.googlecode.barongreenback.shared.RecordDefinition;
import com.googlecode.barongreenback.shared.Repository;
import com.googlecode.barongreenback.views.Views;
import com.googlecode.funclate.Model;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.totallylazy.Sequences;
import com.googlecode.totallylazy.records.Keyword;
import com.googlecode.totallylazy.records.Record;
import com.googlecode.totallylazy.records.Records;
import com.googlecode.utterlyidle.MediaType;
import com.googlecode.utterlyidle.Response;
import com.googlecode.utterlyidle.annotations.DefaultValue;
import com.googlecode.utterlyidle.annotations.FormParam;
import com.googlecode.utterlyidle.annotations.GET;
import com.googlecode.utterlyidle.annotations.POST;
import com.googlecode.utterlyidle.annotations.Path;
import com.googlecode.utterlyidle.annotations.Produces;
import com.googlecode.utterlyidle.annotations.QueryParam;
import org.apache.lucene.queryParser.ParseException;

import java.net.URL;
import java.util.UUID;

import static com.googlecode.barongreenback.shared.RecordDefinition.uniqueFields;
import static com.googlecode.barongreenback.views.View.view;
import static com.googlecode.funclate.Model.model;
import static com.googlecode.totallylazy.Strings.EMPTY;
import static com.googlecode.totallylazy.records.Keywords.keyword;
import static com.googlecode.totallylazy.records.Keywords.keywords;
import static com.googlecode.totallylazy.records.RecordMethods.update;
import static com.googlecode.totallylazy.records.Using.using;
import static com.googlecode.utterlyidle.proxy.Resource.redirect;
import static com.googlecode.utterlyidle.proxy.Resource.resource;

@Path("crawler")
@Produces(MediaType.TEXT_HTML)
public class CrawlerResource {
    public static final String NUMBER_OF_FIELDS = "3";
    private final Records records;
    private final Repository<UUID, Model> modelRepository;
    private final Crawler crawler;
    private final Views views;

    public CrawlerResource(final Records records, final Repository<UUID, Model> modelRepository, Crawler crawler, Views views) {
        this.records = records;
        this.modelRepository = modelRepository;
        this.crawler = crawler;
        this.views = views;
    }

    @GET
    @Path("edit")
    public Model edit(@QueryParam("id") String id, @QueryParam("numberOfFields") @DefaultValue(NUMBER_OF_FIELDS) Integer numberOfFields) {
        return addTemplates(modelRepository.get(UUID.fromString(id)));
    }

    @POST
    @Path("edit")
    public Response edit(@QueryParam("numberOfFields") @DefaultValue(NUMBER_OF_FIELDS) Integer numberOfFields, @FormParam("action") String action,
                         @FormParam("update") String update, @FormParam("from") URL from, RecordDefinition recordDefinition) throws Exception {
        return crawl(numberOfFields, action, update, from, recordDefinition);
    }

    @GET
    @Path("new")
    public Model get(@QueryParam("numberOfFields") @DefaultValue(NUMBER_OF_FIELDS) Integer numberOfFields) {
        return emptyForm(numberOfFields);
    }

    @POST
    @Path("new")
    public Response crawl(@QueryParam("numberOfFields") @DefaultValue(NUMBER_OF_FIELDS) Integer numberOfFields, @FormParam("action") String action,
                          @FormParam("update") String update, @FormParam("from") URL from, RecordDefinition recordDefinition) throws Exception {
        if (action.equals("Save")) {
            UUID key = UUID.randomUUID();
            modelRepository.set(key, toModel(update, from, recordDefinition));
            return redirect(resource(getClass()).edit(key.toString(), numberOfFields));
        }
        Sequence<Record> extractedValues = crawler.crawl(from, recordDefinition);
        return put(keyword(update), uniqueFields(recordDefinition), extractedValues);
    }


    private Model emptyForm(Integer numberOfFields) {
        return addTemplates(form("", "", emptyDefinition(numberOfFields(numberOfFields))));
    }

    private Model addTemplates(Model model) {
        return model.add("emptyKeyword", emptyKeyword());
    }

    private int numberOfFields(Integer numberOfFields) {
        return Math.min(Math.max(numberOfFields, 1), 100);
    }

    private Model emptyDefinition(int number) {
        return RecordDefinition.recordDefinition("", Sequences.repeat(emptyKeyword()).take(number).toArray(Model.class));
    }

    private Model emptyKeyword() {
        return model().add("visible", true);
    }

    private Model form(String update, String from, Model definition) {
        return model().
                add("form", model().
                        add("update", update).
                        add("from", from).
                        add("record", definition));
    }

    private Model toModel(String update, URL from, RecordDefinition recordDefinition) {
        return form(update, from.toString(), recordDefinition.toModel());
    }

    private Response put(final Keyword<Object> recordName, Sequence<Keyword> unique, final Sequence<Record> recordsToAdd) throws ParseException {
        views.put(view(recordName).withFields(keywords(recordsToAdd)));
        records.put(recordName, update(using(unique), recordsToAdd));
        return redirect(resource(SearchResource.class).find(recordName.name(), EMPTY));
    }
}
