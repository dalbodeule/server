package space.mori.server.zombie

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import space.mori.server.proto.Event
import space.mori.server.proto.EventServiceGrpc
import java.util.logging.Logger

class Zombie : JavaPlugin(), Listener {
    companion object {
        lateinit var instance: Zombie
        lateinit var Logger: Logger
        lateinit var channel: ManagedChannel
        lateinit var stub: EventServiceGrpc.EventServiceStub
        lateinit var PlayerJoinRequestObserver: StreamObserver<Event.PlayerJoinRequest>
    }

    override fun onEnable() {
        instance = this
        Logger = this.logger

        channel = ManagedChannelBuilder
            .forAddress("localhost", 50000)
            .usePlaintext()
            .build()

        stub = EventServiceGrpc.newStub(channel)

        PlayerJoinRequestObserver = stub.playerJoin(Observer())

        server.pluginManager.run {
            this.registerEvents(this@Zombie, this@Zombie)
        }
    }

    override fun onDisable() {
        channel.shutdownNow()
    }

    @EventHandler
    internal fun onPlayerJoin(event: PlayerJoinEvent) {
        val uuid = event.player.uniqueId.toString()
        val username = event.player.displayName

        val context = Event.PlayerJoinRequest.newBuilder()
            .setUsername(username).setUuid(uuid)
            .build()

        PlayerJoinRequestObserver.onNext(context)

        event.joinMessage = ""
    }
}

class Observer : StreamObserver<Event.PlayerJoinResponse> {
    override fun onNext(value: Event.PlayerJoinResponse?) {
        val uuid = value?.uuid
        val username = value?.username

        if (uuid != null && username != null)
            Zombie.instance.server.broadcastMessage("$username $uuid is join this server")
    }

    override fun onError(t: Throwable?) {
        if (t != null) throw t
    }

    override fun onCompleted() {

    }
}