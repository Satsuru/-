package by.bsuir.platform.payment;

import by.bsuir.platform.contracts.Rpc;
import by.bsuir.platform.contracts.grpc.Empty;
import by.bsuir.platform.contracts.grpc.PaymentApiGrpc;
import by.bsuir.platform.contracts.grpc.PaymentRequest;
import io.grpc.stub.StreamObserver;

import java.math.BigDecimal;

/** gRPC-сервер оплаты. Запись платежа делает PaymentService. */
final class PaymentGrpcService extends PaymentApiGrpc.PaymentApiImplBase {

    private final PaymentService payments;

    PaymentGrpcService(PaymentService payments) {
        this.payments = payments;
    }

    @Override
    public void complete(PaymentRequest request, StreamObserver<Empty> observer) {
        Rpc.reply(observer, () -> {
            payments.complete(request.getBookingId(), request.getUserId(), new BigDecimal(request.getAmount()), request.getMethod());
            return Empty.getDefaultInstance();
        });
    }
}
