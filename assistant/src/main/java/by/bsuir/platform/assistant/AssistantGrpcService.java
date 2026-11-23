package by.bsuir.platform.assistant;

import by.bsuir.platform.contracts.ProtoMaps;
import by.bsuir.platform.contracts.Rpc;
import by.bsuir.platform.contracts.grpc.AskRequest;
import by.bsuir.platform.contracts.grpc.AssistantApiGrpc;
import by.bsuir.platform.contracts.grpc.DialogView;
import io.grpc.stub.StreamObserver;

/** gRPC-сервер помощника. Текст ответа приходит уже готовым, сервис его только сохраняет. */
final class AssistantGrpcService extends AssistantApiGrpc.AssistantApiImplBase {

    private final AssistantService assistant;

    AssistantGrpcService(AssistantService assistant) {
        this.assistant = assistant;
    }

    @Override
    public void ask(AskRequest request, StreamObserver<DialogView> observer) {
        Rpc.reply(observer, () -> ProtoMaps.dialog(assistant.ask(
                request.getUserId(),
                request.getHasDialog() ? request.getDialogId() : null,
                request.getGuestText(),
                request.getAnswer(),
                request.getEscalated()
        )));
    }
}
