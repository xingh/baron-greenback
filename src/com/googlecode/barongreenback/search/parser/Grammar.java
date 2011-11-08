package com.googlecode.barongreenback.search.parser;

import com.googlecode.lazyparsec.Parser;
import com.googlecode.lazyparsec.Parsers;
import com.googlecode.lazyparsec.pattern.CharacterPredicates;
import com.googlecode.totallylazy.Callable1;
import com.googlecode.totallylazy.Predicate;
import com.googlecode.totallylazy.Predicates;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.totallylazy.Triple;
import com.googlecode.totallylazy.predicates.LogicalPredicate;
import com.googlecode.totallylazy.records.Keyword;
import com.googlecode.totallylazy.records.Record;

import java.util.List;

import static com.googlecode.lazyparsec.Scanners.isChar;
import static com.googlecode.totallylazy.Predicates.and;
import static com.googlecode.totallylazy.Predicates.or;
import static com.googlecode.totallylazy.Predicates.where;
import static com.googlecode.totallylazy.Sequences.sequence;
import static com.googlecode.totallylazy.records.Keywords.keyword;

@SuppressWarnings("unchecked")
public class Grammar {
    public static final Parser<String> TEXT = isChar(CharacterPredicates.IS_ALPHA_NUMERIC).many1().source();
    public static final Parser<String> QUOTED_TEXT = isChar(CharacterPredicates.IS_ALPHA_NUMERIC).or(isChar(' ')).many1().source().
            between(isChar('"'), isChar('"'));
    public static final Parser<String> VALUE = QUOTED_TEXT.or(TEXT);
    public static final Parser<List<String>> VALUES = VALUE.sepBy(isChar(','));
    public static final Parser<String> NAME = VALUE;

    public static Parser<Predicate<Record>> VALUE_ONLY(final Sequence<Keyword> keywords) {
        return VALUES.map(new Callable1<List<String>, Predicate<Record>>() {
            public Predicate<Record> call(final List<String> values) throws Exception {
                return or(keywords.map(new Callable1<Keyword, Predicate>() {
                    public Predicate call(final Keyword keyword) throws Exception {
                        return matchesValues(keyword, values);
                    }
                }).toArray(Predicate.class));
            }
        });
    }

    private static LogicalPredicate matchesValues(final Keyword keyword, List<String> values) {
        return or(sequence(values).map(new Callable1<String, Predicate>() {
            public Predicate call(String value) throws Exception {
                return where(keyword, Predicates.is(value));
            }
        }).toArray(Predicate.class));
    }

    public static final Parser<Callable1<Predicate<Record>, Predicate<Record>>> NEGATION = isChar('-').optional().map(new Callable1<Void, Callable1<Predicate<Record>, Predicate<Record>>>() {
        public Callable1<Predicate<Record>, Predicate<Record>> call(Void aVoid) throws Exception {
            return new Callable1<Predicate<Record>, Predicate<Record>>() {
                public Predicate<Record> call(Predicate<Record> recordPredicate) throws Exception {
                    return Predicates.not(recordPredicate);
                }
            };
        }
    });

    public static Parser<Predicate<Record>> PARTS(final Sequence<Keyword> keywords) {
        return Parsers.or(NAME_AND_VALUE, VALUE_ONLY(keywords)).prefix(NEGATION);
    }

    public static Parser<Predicate<Record>> PARSER(final Sequence<Keyword> keywords) {
        return PARTS(keywords).followedBy(isChar(' ').optional()).many().map(new Callable1<List<Predicate<Record>>, Predicate<Record>>() {
            public Predicate<Record> call(final List<Predicate<Record>> predicates) throws Exception {
                return and(predicates.toArray(new Predicate[0]));
            }
        });
    }

    public static final Parser<Predicate<Record>> NAME_AND_VALUE = Parsers.tuple(NAME, isChar(':'), VALUES).map(new Callable1<Triple<String, Void, List<String>>, Predicate<Record>>() {
        public Predicate<Record> call(Triple<String, Void, List<String>> triple) throws Exception {
            final String name = triple.first();
            final List<String> values = triple.third();
            return matchesValues(keyword(name, String.class), values);
        }
    });


}
