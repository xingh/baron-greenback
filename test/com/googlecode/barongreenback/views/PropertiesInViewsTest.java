package com.googlecode.barongreenback.views;

import com.googlecode.barongreenback.EndToEndTest;
import com.googlecode.barongreenback.search.ViewSearchPage;
import com.googlecode.barongreenback.shared.ApplicationTests;
import com.googlecode.totallylazy.matchers.NumberMatcher;
import com.googlecode.waitrest.Waitrest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static com.googlecode.barongreenback.crawler.CrawlerTests.serverWithDataFeed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PropertiesInViewsTest extends ApplicationTests {
    private Waitrest waitrest;

    @Before
    public void setupFeed() throws Exception {
        waitrest = serverWithDataFeed();
    }

    @After
    public void shutDownFeed() throws Exception {
        waitrest.close();
    }

    @Override
    protected Properties getProperties() {
        Properties properties = new Properties();
        properties.setProperty("user.name", "Matt");
        return properties;
    }

    @Test
    public void supportsPropertiesInQuery() throws Exception {
        String crawlerName = "test";
        crawlSampleData(importCrawler(EndToEndTest.class.getResourceAsStream("testQueuesCrawler.json")), crawlerName);

        ViewEditPage edit = new ViewEditPage(browser, new ViewListPage(browser).link(crawlerName).click());
        edit.query().value("firstName:$properties(\"user.name\")$");
        edit.save();

        ViewSearchPage viewSearchPage = view(crawlerName);

        assertThat(viewSearchPage.resultsSize(), NumberMatcher.is(1));
        assertThat(viewSearchPage.containsCell("firstName", 0, "Matt"), is(true));
        assertThat(viewSearchPage.containsCell("firstName", 1, "Dan"), is(false));
    }


    private ViewSearchPage view(String name) throws Exception {
        return new ViewSearchPage(browser, name, "");
    }

}
