package space.mori.server.api

import io.grpc.ManagedChannelBuilder
import org.apache.logging.log4j.kotlin.logger
import space.mori.server.proto.Test
import space.mori.server.proto.TestServiceGrpc
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val logger = logger("Client")
    val channel = ManagedChannelBuilder
        .forAddress("localhost", 50000)
        .usePlaintext().build()

    val stub = TestServiceGrpc.newBlockingStub(channel)

    val response = stub.sayHello(getHelloRequest("Hello~~"))

    logger.info("response: ${response.greeting}")

    channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
}

fun getHelloRequest(greeting: String): Test.HelloRequest {
    return Test.HelloRequest.newBuilder()
        .setGreeting(greeting).build()
}