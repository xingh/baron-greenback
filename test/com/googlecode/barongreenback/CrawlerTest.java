package com.googlecode.barongreenback;

import com.googlecode.totallylazy.Sequence;
import com.googlecode.totallylazy.records.Keyword;
import com.googlecode.totallylazy.records.Record;
import com.googlecode.utterlyidle.Server;
import com.googlecode.utterlyidle.io.Url;
import org.junit.Test;

import java.util.Date;

import static com.googlecode.totallylazy.URLs.packageUrl;
import static com.googlecode.utterlyidle.ApplicationBuilder.application;
import static com.googlecode.utterlyidle.ServerConfiguration.defaultConfiguration;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CrawlerTest {
    private static final Keyword<Object> ENTRIES = Keyword.keyword("/feed/entry");
    private static final Keyword<String> ID = Keyword.keyword("id", String.class);
    private static final Keyword<String> LINK = Keyword.keyword("link/@href", String.class);
    private static final Keyword<Date> UPDATED = Keyword.keyword("updated", Date.class);
    private static final Keyword<String> TITLE = Keyword.keyword("title", String.class);

    private static final Keyword<Object> USER = Keyword.keyword("/user");
    private static final Keyword<Integer> USER_ID = Keyword.keyword("summary/userId", Integer.class);
    private static final Keyword<String> FIRST_NAME = Keyword.keyword("summary/firstName", String.class);

    @Test
    public void shouldGetTheContentsOfAUrlAndExtractContent() throws Exception {
        Server server = startServer();
        final Url feed = createFeed(server);
        Sequence<Record> records = crawl(feed);
        Record entry = records.head();

        assertThat(entry.get(ID), is("urn:uuid:c356d2c5-f975-4c4d-8e2a-a698158c6ef1"));
        assertThat(entry.get(USER_ID), is(1234));
        assertThat(entry.get(FIRST_NAME), is("Dan"));
        server.close();
    }

    public static Sequence<Record> crawl(Url feed) throws Exception {
        final Crawler crawler = new Crawler();
        return crawler.crawl(new XmlSource(feed.toURL(), ENTRIES, ID, LINK, UPDATED, TITLE)).
                flatMap(crawler.crawl(LINK, USER, USER_ID, FIRST_NAME));
    }

    public static Url createFeed(final Server server) {
        Url base = server.getUrl();
        return base.replacePath(base.path().subDirectory("static").file("atom.xml"));
    }


    public static Server startServer() {
        return application().content(packageUrl(CrawlerTest.class), "static").start(defaultConfiguration().port(10010));
    }

}
