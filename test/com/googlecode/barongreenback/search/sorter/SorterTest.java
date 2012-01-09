package com.googlecode.barongreenback.search.sorter;

import com.googlecode.totallylazy.Callable1;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.totallylazy.Sequences;
import com.googlecode.totallylazy.Triple;
import com.googlecode.lazyrecords.Keyword;
import com.googlecode.lazyrecords.MapRecord;
import com.googlecode.lazyrecords.Record;
import com.googlecode.utterlyidle.RequestBuilder;
import org.junit.Test;

import java.util.Date;

import static com.googlecode.barongreenback.search.sorter.Sorter.SORT_COLUMN_QUERY_PARAM;
import static com.googlecode.barongreenback.search.sorter.Sorter.SORT_DIRECTION_QUERY_PARAM;
import static com.googlecode.totallylazy.Sequences.sequence;
import static com.googlecode.totallylazy.Triple.triple;
import static com.googlecode.lazyrecords.Keywords.keyword;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SorterTest {

    @Test
    public void readsKeywordToSortFromRequest() throws Exception {
        Sorter secondSorter = new Sorter(RequestBuilder.get("/somePath").withQuery(SORT_COLUMN_QUERY_PARAM, "mooCow").build());
        assertThat(secondSorter.sort(records(), keywords()).map(asSecondKeyword()).safeCast(String.class), is(sequence("1", "2", "3")));

        Sorter thirdSorter = new Sorter(RequestBuilder.get("/somePath").withQuery(SORT_COLUMN_QUERY_PARAM, "blueCow").build());
        assertThat(thirdSorter.sort(records(), keywords()).map(asThirdKeyword()).safeCast(Date.class), is(sequence(new Date(1), new Date(10000), new Date(100000))));
    }

    @Test
    public void defaultsToFirstColumnWhenKeywordMissing() throws Exception {
        Sorter sorterWithNoParam = new Sorter(RequestBuilder.get("/somePath").build());
        assertThat(sorterWithNoParam.sort(records(), keywords()).map(asFirstKeyword()).safeCast(String.class), is(sequence("A", "B", "C")));
    }

    @Test
    public void usesDescendingOrderWhenSpecified() throws Exception {
        Sorter secondSorter = new Sorter(RequestBuilder.get("/somePath").withQuery(SORT_COLUMN_QUERY_PARAM, "mooCow").withQuery(SORT_DIRECTION_QUERY_PARAM, "desc").build());
        assertThat(secondSorter.sort(records(), keywords()).map(asSecondKeyword()).safeCast(String.class), is(sequence("3", "2", "1")));
    }

    private Callable1<? super Record, Object> asFirstKeyword() {
        return new Callable1<Record, Object>() {
            public Object call(Record record) throws Exception {
                return record.get(keywords().first());
            }
        };
    }

    private Callable1<? super Record, Object> asThirdKeyword() {
        return new Callable1<Record, Object>() {
            public Object call(Record record) throws Exception {
                return record.get(keywords().last());
            }
        };
    }

    private Callable1<Record, Object> asSecondKeyword() {
        return new Callable1<Record, Object>() {
            public Object call(Record record) throws Exception {
                return record.get(keywords().second());
            }
        };
    }

    private Sequence<Keyword> keywords() {
        return Sequences.<Keyword>sequence(keyword("stuCow"), keyword("mooCow"), keyword("blueCow", Date.class));
    }

    private Sequence<Record> records() {
        Sequence<Triple<String, String, Date>> baseRecords = sequence(triple("A", "2", new Date(100000)), triple("B", "3", new Date(10000)), triple("C", "1", new Date(1)));

        return baseRecords.map(new Callable1<Triple<String, String, Date>, Record>() {
            public Record call(Triple<String, String, Date> triple) throws Exception {
                Record record = MapRecord.record();
                record.set(keywords().first(), triple.first());
                record.set(keywords().second(), triple.second());
                record.set(keywords().last(), triple.third());
                return record;
            }
        });
    };
}
