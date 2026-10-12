package by.bsuir.platform.common;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

@Component
public class GraphQlErrors extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof NotFoundException) {
            return error(ex, env, ErrorType.NOT_FOUND);
        }
        if (ex instanceof BadRequestException) {
            return error(ex, env, ErrorType.BAD_REQUEST);
        }
        if (ex instanceof UnauthorizedException) {
            return error(ex, env, ErrorType.UNAUTHORIZED);
        }
        return null;
    }

    private GraphQLError error(Throwable ex, DataFetchingEnvironment env, ErrorType type) {
        return GraphqlErrorBuilder.newError(env).message(ex.getMessage()).errorType(type).build();
    }
}
