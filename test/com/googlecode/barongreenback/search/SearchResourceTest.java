package com.googlecode.barongreenback.search;

import com.googlecode.barongreenback.crawler.CrawlerTest;
import com.googlecode.barongreenback.WebApplication;
import com.googlecode.barongreenback.views.View;
import com.googlecode.barongreenback.views.Views;
import com.googlecode.totallylazy.Callable1;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.totallylazy.records.ImmutableKeyword;
import com.googlecode.totallylazy.records.Keyword;
import com.googlecode.totallylazy.records.Keywords;
import com.googlecode.totallylazy.records.Record;
import com.googlecode.totallylazy.records.lucene.LuceneRecords;
import com.googlecode.totallylazy.records.xml.Xml;
import com.googlecode.totallylazy.records.xml.XmlRecords;
import com.googlecode.utterlyidle.Application;
import com.googlecode.utterlyidle.Response;
import com.googlecode.utterlyidle.Server;
import com.googlecode.utterlyidle.httpserver.RestServer;
import com.googlecode.utterlyidle.io.Url;
import com.googlecode.yadic.Container;
import org.junit.Test;

import static com.googlecode.totallylazy.Runnables.VOID;
import static com.googlecode.totallylazy.matchers.IterableMatcher.hasExactly;
import static com.googlecode.totallylazy.records.Keywords.keyword;
import static com.googlecode.totallylazy.records.Keywords.keywords;
import static com.googlecode.utterlyidle.ApplicationBuilder.application;
import static com.googlecode.utterlyidle.RequestBuilder.get;
import static com.googlecode.utterlyidle.ServerConfiguration.defaultConfiguration;
import static com.googlecode.utterlyidle.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

public class SearchResourceTest {
    @Test
    public void canQuery() throws Exception {
        Response response = application(addSomeData(new WebApplication())).handle(get("users/search/list").withQuery("query", "type:users"));
        assertThat(response.status(), is(OK));

        XmlRecords xmlRecords = new XmlRecords(Xml.load(new String(response.bytes())));
        Keyword results = keyword("//table[@class='results']/tbody/tr");
        Keyword<String> id = keyword("td[@class='id']", String.class);
        xmlRecords.define(results, id);
        Sequence<String> result = xmlRecords.get(results).map(id);
        assertThat(result, hasExactly("urn:uuid:c356d2c5-f975-4c4d-8e2a-a698158c6ef1", "urn:uuid:c356d2c5-f975-4c4d-8e2a-a698158c6ef2"));
    }

    public static Application addSomeData(final WebApplication application) throws Exception {
        Server server = CrawlerTest.startServer();
        Url feed = CrawlerTest.createFeed(server);
        final Sequence<Record> recordSequence = CrawlerTest.crawl(feed).realise();

        application.usingRequestScope(new Callable1<Container, Void>() {
            public Void call(Container container) throws Exception {
                LuceneRecords luceneRecords = container.get(LuceneRecords.class);
                Keyword<Object> users = keyword("users");
                luceneRecords.define(users, keywords(recordSequence).toArray(Keyword.class));
                luceneRecords.add(users, recordSequence);
                Views views = container.get(Views.class);
                views.add(View.view(users).withFields(keywords(recordSequence)));
                return VOID;
            }
        });
        server.close();
        return application;
    }

    public static void main(String[] args) throws Exception {
        new RestServer(addSomeData(new WebApplication()), defaultConfiguration().port(9000));
    }
}
