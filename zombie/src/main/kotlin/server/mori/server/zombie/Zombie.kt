package server.mori.server.zombie

import io.grpc.ManagedChannelBuilder
import org.bukkit.plugin.java.JavaPlugin
import space.mori.server.proto.Test
import space.mori.server.proto.TestServiceGrpc
import java.util.logging.Logger

class Zombie : JavaPlugin() {
    companion object {
        lateinit var instance: Zombie
        lateinit var Logger: Logger
        lateinit var stub: TestServiceGrpc.TestServiceBlockingStub
    }

    override fun onEnable() {
        instance = this
        Logger = this.logger

        val channel = ManagedChannelBuilder
            .forAddress("localhost", 50000)
            .usePlaintext()
            .build()

        stub = TestServiceGrpc.newBlockingStub(channel)

        val response = stub.sayHello(Test.HelloRequest.newBuilder().setGreeting("hello~").build())

        logger.info("response: ${response.greeting}")
    }

    override fun onDisable() {

    }
}