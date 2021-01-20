package space.mori.server.api.services

import io.grpc.stub.StreamObserver
import space.mori.server.proto.Test
import space.mori.server.proto.TestServiceGrpc

class TestService : TestServiceGrpc.TestServiceImplBase() {
    override fun sayHello(request: Test.HelloRequest?, responseObserver: StreamObserver<Test.HelloResponse>?) {
        val response = Test.HelloResponse.newBuilder().setGreeting(request?.greeting ?: "hello").build()

        responseObserver?.onNext(response)
        responseObserver?.onCompleted()
    }
}
