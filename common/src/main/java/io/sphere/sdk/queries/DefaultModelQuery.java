package io.sphere.sdk.queries;

import com.fasterxml.jackson.core.type.TypeReference;

public class DefaultModelQuery<I> extends QueryDslImpl<I> {
    public DefaultModelQuery(final String endpoint, final TypeReference<PagedQueryResult<I>> pagedQueryResultTypeReference) {
        super(endpoint, pagedQueryResultTypeReference);
    }
}