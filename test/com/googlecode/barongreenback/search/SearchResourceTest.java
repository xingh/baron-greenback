package com.googlecode.barongreenback.search;

import com.googlecode.barongreenback.crawler.CompositeCrawlerTest;
import com.googlecode.barongreenback.crawler.CrawlerTests;
import com.googlecode.barongreenback.persistence.BaronGreenbackRecords;
import com.googlecode.barongreenback.shared.ApplicationTests;
import com.googlecode.barongreenback.shared.ModelRepository;
import com.googlecode.barongreenback.views.ViewsRepository;
import com.googlecode.funclate.Model;
import com.googlecode.lazyrecords.Definition;
import com.googlecode.lazyrecords.Keyword;
import com.googlecode.lazyrecords.Keywords;
import com.googlecode.lazyrecords.Record;
import com.googlecode.lazyrecords.Records;
import com.googlecode.totallylazy.Block;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.totallylazy.Strings;
import com.googlecode.utterlyidle.RequestBuilder;
import com.googlecode.utterlyidle.Response;
import com.googlecode.utterlyidle.Status;
import com.googlecode.waitrest.Waitrest;
import com.googlecode.yadic.Container;
import com.googlecode.yatspec.junit.Row;
import com.googlecode.yatspec.junit.SpecRunner;
import com.googlecode.yatspec.junit.Table;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

import static com.googlecode.lazyrecords.Definition.constructors.definition;
import static com.googlecode.lazyrecords.Keyword.constructors.keyword;
import static com.googlecode.lazyrecords.Keyword.methods.keywords;
import static com.googlecode.lazyrecords.Record.constructors.record;
import static com.googlecode.totallylazy.matchers.NumberMatcher.is;
import static com.googlecode.totallylazy.proxy.Call.method;
import static com.googlecode.totallylazy.proxy.Call.on;
import static com.googlecode.utterlyidle.HttpHeaders.LOCATION;
import static com.googlecode.utterlyidle.MediaType.APPLICATION_JSON;
import static com.googlecode.utterlyidle.RequestBuilder.get;
import static com.googlecode.utterlyidle.Response.methods.header;
import static com.googlecode.utterlyidle.annotations.AnnotatedBindings.relativeUriOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@RunWith(SpecRunner.class)
public class SearchResourceTest extends ApplicationTests {

    private Definition usersView;

    @Before
    public void addSomeData() throws Exception {
        Waitrest waitrest = CrawlerTests.serverWithDataFeed();
        final Sequence<Record> recordSequence = CompositeCrawlerTest.crawlOnePageOnly(feed(), feedClient()).realise();

        application.usingRequestScope(new Block<Container>() {
            public void execute(Container container) throws Exception {
                Records records = container.get(BaronGreenbackRecords.class).value();
                usersView = definition("users", keywords(recordSequence).append(keyword("updated", Date.class)));
                records.add(usersView, recordSequence);
                ModelRepository views = container.get(ModelRepository.class);
                views.set(UUID.randomUUID(), ViewsRepository.convertToViewModel(usersView));
            }
        });
        waitrest.close();
    }

    @Test
    public void handlesInvalidQueriesInANiceWay() throws Exception {
        SearchPage searchPage = new SearchPage(browser, "users", "^&%$^%");
        assertThat(searchPage.queryMessage(), Matchers.is("Invalid Query"));
    }

    @Test
    public void supportsDelete() throws Exception {
        SearchPage searchPage = new SearchPage(browser, "users", "", true);
        assertThat(searchPage.numberOfResults(), is(3));
        searchPage = searchPage.delete();
        assertThat(searchPage.numberOfResults(), is(0));
    }

    @Test
    public void supportsQueryAll() throws Exception {
        SearchPage searchPage = new SearchPage(browser, "users", "");
        assertThat(searchPage.numberOfResults(), is(3));
    }

    @Test
    public void supportsQueryForAParticularEntry() throws Exception {
        SearchPage searchPage = new SearchPage(browser, "users", "id:\"urn:uuid:c356d2c5-f975-4c4d-8e2a-a698158c6ef1\"");
        assertThat(searchPage.numberOfResults(), is(1));
    }

    @Test
    @Table({
            @Row({"2011/02/19"}),
            @Row({"19/02/11"}),
            @Row({"19/02/2011"})
    })
    public void supportsDateQuery(String query) throws Exception {
        SearchPage searchPage = new SearchPage(browser, "users", query);
        assertThat(searchPage.numberOfResults(), is(2));
    }

    @Test
    @Table({
            @Row({"2011/02/19 12:43:25"}),
            @Row({"19/02/11 12:43:25"}),
            @Row({"19/02/2011 12:43:25"})
    })
    public void supportsDateTimeQuery(String query) throws Exception {
        SearchPage searchPage = new SearchPage(browser, "users", query);
        assertThat(searchPage.numberOfResults(), is(1));
    }

    @Test
    @Table({
            @Row({"2011/07/19 13:43:25"}),
            @Row({"19/07/11 13:43:25"}),
            @Row({"19/07/2011 13:43:25"})
    })
    public void supportsDateTimeQueriesFormattedInSummerTime(String query) throws Exception {
        SearchPage searchPage = new SearchPage(browser, "users", query);
        assertThat(searchPage.numberOfResults(), is(1));
    }

    @Test
    public void whenAnUnknownViewIsSpecifiedThenNoResultsShouldBeShown() throws Exception {
        SearchPage searchPage = new SearchPage(browser, "UNKNOWN", "");
        assertThat(searchPage.numberOfResults(), is(0));
    }

    @Test
    public void supportsShortcutToUniquePage() throws Exception {
        RequestBuilder requestBuilder = get("/" + relativeUriOf(method(on(SearchResource.class).shortcut("users", "id:\"urn:uuid:c356d2c5-f975-4c4d-8e2a-a698158c6ef1\""))));
        Response response = application.handle(requestBuilder.build());
        assertThat(response.status(), Matchers.is(Status.SEE_OTHER));
        assertThat(header(response, LOCATION), Matchers.is("/users/search/unique?query=id%3A%22urn%3Auuid%3Ac356d2c5-f975-4c4d-8e2a-a698158c6ef1%22"));
    }

    @Test
    public void supportsShortcutToListPage() throws Exception {
        RequestBuilder requestBuilder = get("/" + relativeUriOf(method(on(SearchResource.class).shortcut("users", ""))));
        Response response = application.handle(requestBuilder.build());
        assertThat(response.status(), Matchers.is(Status.SEE_OTHER));
        assertThat(header(response, LOCATION), Matchers.is("/users/search/list?query="));
    }

    @Test
    public void shortCut() throws Exception {
        RequestBuilder requestBuilder = get("/" + relativeUriOf(method(on(SearchResource.class).shortcut("users", "BAD_QUERY"))));
        Response response = application.handle(requestBuilder.build());
        assertThat(response.status(), Matchers.is(Status.SEE_OTHER));
        assertThat(header(response, LOCATION), Matchers.is("/users/search/list?query=BAD_QUERY"));
    }

    @Test
    public void canExportToCsv() throws Exception {
        RequestBuilder requestBuilder = get("/" + relativeUriOf(method(on(SearchResource.class).exportCsv("users", "first:Dan OR first:Matt"))));
        Response response = application.handle(requestBuilder.build());
        InputStream resourceAsStream = SearchResourceTest.class.getResourceAsStream("csvTest.csv");
        String expected = Strings.toString(resourceAsStream);
        assertThat(response.entity().toString(), Matchers.is(expected));
    }

    @Test
    public void shouldExportOnlyVisibleFields() throws Exception {
        Keyword<?> invisibleField = usersView.fields().first();
        invisibleField.metadata().set(ViewsRepository.VISIBLE, Boolean.FALSE);

        RequestBuilder requestBuilder = get("/" + relativeUriOf(method(on(SearchResource.class).exportCsv("users", "first:Dan OR first:Matt"))));
        Response response = application.handle(requestBuilder.build());
        assertThat(response.entity().toString(), not(containsString(invisibleField.name())));
    }

    @Test
    public void shouldReturnACorrectlySortedRecord() throws Exception {
        final Model viewDefinition = Model.persistent.parse(Strings.toString(SearchResourceTest.class.getResourceAsStream("testViewDefinition.json")));
        final Definition recordDefinition = definition("items", keyword("fielda").metadata(Keywords.unique, true), keyword("fieldb"), keyword("fieldc"), keyword("fieldd"));
        final Record record = record(keyword("fielda"), "a", keyword("fieldb"), "b", keyword("fieldc"), "c", keyword("fieldd"), "d");

        application.usingRequestScope(new Block<Container>() {
            public void execute(Container container) throws Exception {
                container.get(BaronGreenbackRecords.class).value().add(recordDefinition, record);
                container.get(ViewsRepository.class).set(UUID.randomUUID(), viewDefinition);
            }
        });

        final Response response = application.handle(get(relativeUriOf(method(on(SearchResource.class).unique("items", "")))).accepting(APPLICATION_JSON).build());
        final String expectedResponse = "\"record\":{\"groupa\":{\"aliasc\":\"c\",\"aliasa\":\"a\"},\"groupb\":{\"aliasb\":\"b\"},\"Other\":{\"aliasd\":\"d\"}}";
        assertThat(response.entity().toString(), containsString(expectedResponse));
    }
}
