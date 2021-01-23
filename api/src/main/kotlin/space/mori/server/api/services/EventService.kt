package space.mori.server.api.services

import io.grpc.stub.StreamObserver
import space.mori.server.api.Main
import space.mori.server.proto.Event
import space.mori.server.proto.EventServiceGrpc

class EventService : EventServiceGrpc.EventServiceImplBase() {
    val connectedSession: MutableList<StreamObserver<Event.PlayerJoinResponse>> = mutableListOf()

    override fun playerJoin(responseObserver: StreamObserver<Event.PlayerJoinResponse>?): StreamObserver<Event.PlayerJoinRequest> {
        class Observer : StreamObserver<Event.PlayerJoinRequest> {
            override fun onNext(value: Event.PlayerJoinRequest?) {
                val uuid = value?.uuid
                val username = value?.username

                if (responseObserver !in connectedSession && responseObserver != null) {
                    connectedSession.add(responseObserver)
                }

                if (uuid != null && username != null) {
                    val response = Event.PlayerJoinResponse.newBuilder().setUsername(username).setUuid(uuid).build()

                    connectedSession.forEach {
                        it.onNext(response)
                    }
                }
            }

            override fun onCompleted() {
                Main.logger.info("stream complete!")
                if (responseObserver != null) {
                    connectedSession.remove(responseObserver)
                }
            }

            override fun onError(t: Throwable?) {
                if (t != null) {
                    Main.logger.error("Error!")
                    Main.logger.debug(t)
                    Main.logger.trace(t.stackTrace)

                    if (responseObserver != null) {
                        connectedSession.remove(responseObserver)
                    }
                }
            }
        }

        return Observer()
    }
}
