package by.bsuir.platform.notification;

import by.bsuir.platform.contracts.ProtoMaps;
import by.bsuir.platform.contracts.Rpc;
import by.bsuir.platform.contracts.grpc.NotificationApiGrpc;
import by.bsuir.platform.contracts.grpc.NotificationList;
import by.bsuir.platform.contracts.grpc.UserRequest;
import io.grpc.stub.StreamObserver;

/** gRPC только для чтения писем. Новые письма приходят из Kafka. */
final class NotificationGrpcService extends NotificationApiGrpc.NotificationApiImplBase {

    private final NotificationService notifications;

    NotificationGrpcService(NotificationService notifications) {
        this.notifications = notifications;
    }

    @Override
    public void listMine(UserRequest request, StreamObserver<NotificationList> observer) {
        Rpc.reply(observer, () -> ProtoMaps.notifications(notifications.listMine(request.getUserId())));
    }
}
