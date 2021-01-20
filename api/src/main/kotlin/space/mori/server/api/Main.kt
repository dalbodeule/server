package space.mori.server.api

import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import org.apache.logging.log4j.kotlin.logger
import space.mori.server.proto.Test
import space.mori.server.proto.TestServiceGrpc

fun main() {
    Main().run()
}

class Main {
    companion object {
        val logger = logger("server")
    }

    internal fun run() {
        val server = ServerBuilder
            .forPort(50000)
            .addService(TestService())
            .build()

        server.start()
        logger.info("gRPC server started with port ${server.port}")

        server.awaitTermination()
    }
}

class TestService : TestServiceGrpc.TestServiceImplBase() {
    override fun sayHello(request: Test.HelloRequest?, responseObserver: StreamObserver<Test.HelloResponse>?) {
        val response = Test.HelloResponse.newBuilder().setGreeting(request?.greeting ?: "hello").build()

        responseObserver?.onNext(response)
        responseObserver?.onCompleted()
    }
}
