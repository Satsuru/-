package by.bsuir.platform.gateway;

import by.bsuir.platform.contracts.Failures;
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
        if (ex instanceof Failures.NotFoundException) {
            return error(ex, env, ErrorType.NOT_FOUND);
        }
        if (ex instanceof Failures.BadRequestException) {
            return error(ex, env, ErrorType.BAD_REQUEST);
        }
        if (ex instanceof Failures.UnauthorizedException) {
            return error(ex, env, ErrorType.UNAUTHORIZED);
        }
        if (ex instanceof Failures.ForbiddenException) {
            return error(ex, env, ErrorType.FORBIDDEN);
        }
        return null;
    }

    private GraphQLError error(Throwable ex, DataFetchingEnvironment env, ErrorType type) {
        return GraphqlErrorBuilder.newError(env).message(ex.getMessage()).errorType(type).build();
    }
}
