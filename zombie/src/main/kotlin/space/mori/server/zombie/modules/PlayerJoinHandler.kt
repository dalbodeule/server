package space.mori.server.zombie.modules

import io.grpc.stub.StreamObserver
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import space.mori.server.proto.Event
import space.mori.server.proto.EventServiceGrpc
import space.mori.server.zombie.Zombie
import space.mori.server.zombie.Zombie.Companion.channel

object PlayerJoinHandler: Listener {
    private val stub: EventServiceGrpc.EventServiceStub = EventServiceGrpc.newStub(channel)

    private val PlayerJoinRequestObserver: StreamObserver<Event.PlayerJoinRequest> = stub.playerJoin(Observer())

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

    class Observer : StreamObserver<Event.PlayerJoinResponse> {
        override fun onNext(value: Event.PlayerJoinResponse?) {
            if (value != null) {
                val uuid = value.uuid
                val username = value.username

                Zombie.instance.server.broadcastMessage("$username $uuid is join this server")
            }
        }

        override fun onError(t: Throwable?) {
            if (t != null) throw t
        }

        override fun onCompleted() {

        }
    }
}