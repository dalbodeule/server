package space.mori.server.api

import io.grpc.ServerBuilder
import org.apache.logging.log4j.kotlin.logger
import space.mori.server.api.services.EventService
import space.mori.server.api.services.PartyService
import space.mori.server.api.services.TestService

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
            .addService(EventService())
            .addService(PartyService())
            .build()

        server.start()
        logger.info("gRPC server started with port ${server.port}")

        server.awaitTermination()
    }
}
