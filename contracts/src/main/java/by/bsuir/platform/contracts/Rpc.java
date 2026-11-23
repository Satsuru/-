package by.bsuir.platform.contracts;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/** Общая отправка ошибок gRPC и открытие канала. Бизнес-правила здесь не проверяются. */
public final class Rpc {

    private Rpc() {
    }

    public static Server start(int port, BindableService service) {
        try {
            return ServerBuilder.forPort(port).addService(service).build().start();
        } catch (IOException ex) {
            throw new IllegalStateException("Не удалось открыть gRPC-порт " + port, ex);
        }
    }

    public static ManagedChannel channel(String host, int port) {
        return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    }

    public static <S extends AbstractStub<S>> S deadline(S stub) {
        return stub.withDeadlineAfter(5, TimeUnit.SECONDS);
    }

    public static <T> T call(Callable<T> action) {
        try {
            return action.call();
        } catch (RuntimeException ex) {
            throw failure(ex);
        } catch (Exception ex) {
            throw failure(ex);
        }
    }

    public static <T> void reply(StreamObserver<T> observer, Callable<T> action) {
        try {
            observer.onNext(action.call());
            observer.onCompleted();
        } catch (Exception ex) {
            observer.onError(status(ex));
        }
    }

    public static StatusRuntimeException status(Throwable ex) {
        if (ex instanceof StatusRuntimeException status) {
            return status;
        }
        if (ex instanceof Failures.NotFoundException) {
            return Status.NOT_FOUND.withDescription(ex.getMessage()).asRuntimeException();
        }
        if (ex instanceof Failures.BadRequestException) {
            return Status.INVALID_ARGUMENT.withDescription(ex.getMessage()).asRuntimeException();
        }
        String message = ex.getMessage() == null ? "Ошибка сервиса" : ex.getMessage();
        return Status.INTERNAL.withDescription(message).asRuntimeException();
    }

    public static RuntimeException failure(Throwable ex) {
        if (ex instanceof StatusRuntimeException status) {
            String message = status.getStatus().getDescription();
            if (message == null || message.isBlank()) {
                message = "Сервис недоступен";
            }
            return switch (status.getStatus().getCode()) {
                case NOT_FOUND -> new Failures.NotFoundException(message);
                case INVALID_ARGUMENT -> new Failures.BadRequestException(message);
                default -> status;
            };
        }
        if (ex instanceof RuntimeException runtime) {
            return runtime;
        }
        return new IllegalStateException(ex);
    }
}
