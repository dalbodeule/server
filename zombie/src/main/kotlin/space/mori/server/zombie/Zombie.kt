package space.mori.server.zombie

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.bukkit.plugin.java.JavaPlugin
import space.mori.server.zombie.modules.PartyModule
import space.mori.server.zombie.modules.PlayerJoinHandler
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

class Zombie : JavaPlugin() {
    companion object {
        lateinit var instance: Zombie
        lateinit var Logger: Logger
        lateinit var channel: ManagedChannel
    }

    override fun onEnable() {
        instance = this
        Logger = this.logger

        channel = ManagedChannelBuilder
            .forAddress("localhost", 50000)
            .usePlaintext()
            .build()

        server.pluginManager.run {
            this.registerEvents(PlayerJoinHandler, this@Zombie)
        }

        PartyModule.run()
    }

    override fun onDisable() {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
    }
}