/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.query;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class SpanNearQueryParser implements QueryParser {

    public static final String NAME = "span_near";

    @Inject
    public SpanNearQueryParser() {
    }

    @Override
    public String[] names() {
        return new String[]{NAME};
    }

    @Override
    public Query parse(QueryParseContext parseContext) throws IOException, QueryParsingException {
        XContentParser parser = parseContext.parser();

        float boost = 1.0f;
        Integer slop = null;
        boolean inOrder = true;
        boolean collectPayloads = true;
        String queryName = null;

        List<SpanQuery> clauses = new ArrayList<>();

        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token == XContentParser.Token.START_ARRAY) {
                if ("clauses".equals(currentFieldName)) {
                    while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                        Query query = parseContext.parseInnerQuery();
                        if (!(query instanceof SpanQuery)) {
                            throw new QueryParsingException(parseContext, "spanNear [clauses] must be of type span query");
                        }
                        clauses.add((SpanQuery) query);
                    }
                } else {
                    throw new QueryParsingException(parseContext, "[span_near] query does not support [" + currentFieldName + "]");
                }
            } else if (token.isValue()) {
                if ("in_order".equals(currentFieldName) || "inOrder".equals(currentFieldName)) {
                    inOrder = parser.booleanValue();
                } else if ("collect_payloads".equals(currentFieldName) || "collectPayloads".equals(currentFieldName)) {
                    collectPayloads = parser.booleanValue();
                } else if ("slop".equals(currentFieldName)) {
                    slop = Integer.valueOf(parser.intValue());
                } else if ("boost".equals(currentFieldName)) {
                    boost = parser.floatValue();
                } else if ("_name".equals(currentFieldName)) {
                    queryName = parser.text();
                } else {
                    throw new QueryParsingException(parseContext, "[span_near] query does not support [" + currentFieldName + "]");
                }
            } else {
                throw new QueryParsingException(parseContext, "[span_near] query does not support [" + currentFieldName + "]");
            }
        }
        if (clauses.isEmpty()) {
            throw new QueryParsingException(parseContext, "span_near must include [clauses]");
        }
        if (slop == null) {
            throw new QueryParsingException(parseContext, "span_near must include [slop]");
        }

        SpanNearQuery query = new SpanNearQuery(clauses.toArray(new SpanQuery[clauses.size()]), slop.intValue(), inOrder, collectPayloads);
        query.setBoost(boost);
        if (queryName != null) {
            parseContext.addNamedQuery(queryName, query);
        }
        return query;
    }
}
