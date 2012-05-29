package com.googlecode.barongreenback.crawler;

import com.googlecode.totallylazy.Option;
import com.googlecode.totallylazy.Uri;
import org.junit.Test;

import static com.googlecode.totallylazy.Option.none;
import static com.googlecode.totallylazy.Xml.document;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MoreDataCrawlerTest {
    @Test
    public void ifCheckpointFoundReturnNone() throws Exception {
        PaginatedHttpDataSource dataSource = PaginatedHttpDataSource.dataSource(null, null, null, "/root/more", "Today", "/root/date");
        Option<Job> more = dataSource.getMoreIfNeeded(document("<root><date>Today</date></root>"), null);
        assertThat(more, is(none(Job.class)));
    }

    @Test
    public void ifCheckpointNotFoundReturnNextJob() throws Exception {
        PaginatedHttpDataSource dataSource = PaginatedHttpDataSource.dataSource(null, null, null, "/root/more", "Today", "/root/date");
        Option<Job> more = dataSource.getMoreIfNeeded(document("<root><date>Yesterday</date><more>next</more></root>"), null);
        assertThat(more.get().dataSource().uri(), is(Uri.uri("next")));
    }
}
