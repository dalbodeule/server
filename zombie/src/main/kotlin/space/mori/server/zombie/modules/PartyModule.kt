package space.mori.server.zombie.modules

import io.grpc.stub.StreamObserver
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import space.mori.server.proto.Party.PartyStatus.*
import space.mori.server.proto.PartyServiceGrpc
import space.mori.server.zombie.Zombie.Companion.channel
import space.mori.server.zombie.Zombie.Companion.instance
import java.util.*
import space.mori.server.proto.Party as PartyService

object PartyModule: Listener {
    private val stub: PartyServiceGrpc.PartyServiceStub = PartyServiceGrpc.newStub(channel)

    internal fun run() {
        val request = PartyService.PartySubscriptionRequest.newBuilder()
            .build()

        stub.partySubscription(request, Observer())
    }

    @EventHandler
    internal fun playerJoinEvent(event: PlayerJoinEvent) {
        stub.partyInviteUserSearch()
    }

    class Observer : StreamObserver<PartyService.PartySubscriptionStream> {
        override fun onNext(value: PartyService.PartySubscriptionStream?) {
            if (value != null) {
                val uuid = UUID.fromString(value.uuid)
                val displayName = value.displayName
                val partyCode = value.partyCode
                val partyStatus = value.partyStatus
                val message = value.message

                when (partyStatus) {
                    PARTY_JOIN -> {
                        PartyManager.partyJoin(uuid, partyCode)
                    }
                    PARTY_LEAVE -> {
                        PartyManager.partyLeave(uuid)
                    }
                    PARTY_CREATE -> {
                        PartyManager.partyCreate(uuid, partyCode)
                    }
                    PARTY_REMOVE -> {
                        PartyManager.partyRemove(uuid)
                    }
                    PARTY_CHAT -> {
                        instance.server.broadcastMessage("party ${instance.server.getOfflinePlayer(uuid).name} : $message")
                    }
                    PARTY_INVITE -> {
                        if (instance.server.getOfflinePlayer(uuid).player in instance.server.onlinePlayers) {
                            stub.partyInviteUserSearch(PartyService.PartyInviteUserSearchRequest.newBuilder()
                                .setInviterUuid(uuid.toString()).setInviterDisplayName(displayName).setPartyCode(partyCode)
                                .setUsername(message).build(), PartyInviteUserSearchResponse())
                        }
                    }
                    PARTY_INVITE_SUCCESS -> TODO()

                    UNRECOGNIZED -> TODO()
                }
            }
        }

        class PartyInviteUserSearchResponse : StreamObserver<PartyService.PartyInviteUserSearchResponse> {
            override fun onNext(value: PartyService.PartyInviteUserSearchResponse?) {
            }

            override fun onError(t: Throwable?) {

            }

            override fun onCompleted() {

            }
        }

        override fun onError(t: Throwable?) {
            if (t != null) throw t
        }

        override fun onCompleted() {

        }
    }
}

object PartyManager {
    private val PartyList: MutableList<Party> = mutableListOf()
    private val UUIDtoParty: MutableMap<UUID, Party> = mutableMapOf()

    internal fun searchPartyID(uuid: UUID): Int {
        return if (uuid in UUIDtoParty) {
            PartyList.indexOf(UUIDtoParty[uuid])
        } else {
            -1
        }
    }

    internal fun partyCreate(uuid: UUID, partyCode: Int): Int {
        return if (uuid in UUIDtoParty) {
            val party = Party(uuid)

            PartyList[partyCode] = (party)
            UUIDtoParty[uuid] = party

            PartyList.indexOf(party)
        } else {
            -1
        }
    }

    internal fun partyJoin(uuid: UUID, partyID: Int): Boolean {
        return if (uuid in UUIDtoParty && PartyList.getOrNull(partyID) != null) {
            PartyList[partyID].addUser(uuid)
            UUIDtoParty[uuid] = PartyList[partyID]

            true
        } else {
            false
        }
    }

    internal fun partyLeave(uuid: UUID): Boolean {
        return if (uuid in UUIDtoParty) {
            val party = UUIDtoParty[uuid]

            return if (party!!.ownerUUID == uuid) {
                false
            } else {
                party.removeUser(uuid)
                UUIDtoParty.remove(uuid)

                true
            }
        } else {
            false
        }
    }

    internal fun partyRemove(uuid: UUID): Boolean {
        return if (uuid in UUIDtoParty) {
            val party = UUIDtoParty[uuid]

            return if (party!!.ownerUUID == uuid) {
                party.partyUser.forEach {
                    UUIDtoParty.remove(it)
                }
                UUIDtoParty.remove(uuid)

                PartyList.remove(party)

                true
            } else {
                false
            }
        } else {
            false
        }
    }
}

data class Party(
    val ownerUUID: UUID,
    val partyUser: MutableList<UUID> = mutableListOf(ownerUUID)
) {
    internal fun addUser(uuid: UUID): Boolean {
        return when {
            partyUser.size >= 3 -> {
                false
            }
            uuid in partyUser -> {
                false
            }
            else -> {
                partyUser.add(uuid)
                true
            }
        }
    }

    internal fun removeUser(uuid: UUID): Boolean {
        return if (uuid in partyUser && uuid != ownerUUID) {
            partyUser.remove(uuid)
            true
        } else {
            false
        }
    }
}