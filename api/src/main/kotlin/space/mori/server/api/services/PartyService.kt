package space.mori.server.api.services

import io.grpc.stub.StreamObserver
import space.mori.server.api.Main
import space.mori.server.proto.PartyServiceGrpc
import java.util.*
import space.mori.server.proto.Party as PartyService

class PartyService : PartyServiceGrpc.PartyServiceImplBase() {
    private val connectedSession: MutableList<StreamObserver<PartyService.PartySubscriptionStream>> = mutableListOf()
    private val partyInviteStatus: MutableMap<String, Boolean> = mutableMapOf()

    override fun partyCreate(
        request: PartyService.PartyCreateRequest?,
        responseObserver: StreamObserver<PartyService.PartyCreateResponse>?
    ) {
        if (request != null) {
            val uuid = UUID.fromString(request.uuid)
            val displayName = request.displayName

            val result = PartyManager.partyCreate(uuid)

            val response = PartyService.PartyCreateResponse.newBuilder()

            if (result == -1) {
                response.success = false
                response.partyCode = -1
            } else {
                response.success = true
                response.partyCode = result

                partyEventAnnounce(uuid, displayName, result, PartyService.PartyStatus.PARTY_CREATE)
            }

            responseObserver?.onNext(response.build())
            responseObserver?.onCompleted()
        } else {
            responseObserver?.onNext(PartyService.PartyCreateResponse.newBuilder()
                .setSuccess(false).setPartyCode(-1)
                .build())
            responseObserver?.onCompleted()
        }
    }

    override fun partyJoin(
        request: PartyService.PartyJoinRequest?,
        responseObserver: StreamObserver<PartyService.PartyJoinResponse>?
    ) {
        if (request != null) {
            val uuid = UUID.fromString(request.uuid)
            val partyCode = request.partyCode
            val displayName = request.displayName

            val result = PartyManager.partyJoin(uuid, partyCode)

            if (result) partyEventAnnounce(uuid, displayName, partyCode, PartyService.PartyStatus.PARTY_JOIN)

            responseObserver?.onNext(PartyService.PartyJoinResponse.newBuilder()
                .setSuccess(result).build())
            responseObserver?.onCompleted()
        } else {
            responseObserver?.onNext(PartyService.PartyJoinResponse.newBuilder()
                .setSuccess(false).build())
            responseObserver?.onCompleted()
        }
    }

    override fun partyLeave(
        request: PartyService.PartyLeaveRequest?,
        responseObserver: StreamObserver<PartyService.PartyLeaveResponse>?
    ) {
        if (request != null) {
            val uuid = UUID.fromString(request.uuid)
            val partyCode = PartyManager.searchPartyID(uuid)
            val displayName = request.displayName

            val result = PartyManager.partyLeave(uuid)

            if (result) partyEventAnnounce(uuid, displayName, partyCode, PartyService.PartyStatus.PARTY_LEAVE)

            responseObserver?.onNext(PartyService.PartyLeaveResponse.newBuilder()
                .setSuccess(result).build())
            responseObserver?.onCompleted()
        } else {
            responseObserver?.onNext(PartyService.PartyLeaveResponse.newBuilder()
                .setSuccess(false).build())
            responseObserver?.onCompleted()
        }
    }

    override fun partyRemove(
        request: PartyService.PartyRemoveRequest?,
        responseObserver: StreamObserver<PartyService.PartyRemoveResponse>?
    ) {
        if (request != null) {
            val uuid = UUID.fromString(request.uuid)
            val partyCode = PartyManager.searchPartyID(uuid)
            val displayName = request.displayName

            val result = PartyManager.partyRemove(uuid)

            if (result) partyEventAnnounce(uuid, displayName, partyCode, PartyService.PartyStatus.PARTY_REMOVE)

            responseObserver?.onNext(PartyService.PartyRemoveResponse.newBuilder()
                .setSuccess(result).build())
            responseObserver?.onCompleted()
        } else {
            responseObserver?.onNext(PartyService.PartyRemoveResponse.newBuilder()
                .setSuccess(false).build())
            responseObserver?.onCompleted()
        }
    }

    override fun partySubscription(
        request: PartyService.PartySubscriptionRequest?,
        responseObserver: StreamObserver<PartyService.PartySubscriptionStream>?
    ) {
        if (responseObserver !in connectedSession && responseObserver != null) {
            connectedSession.add(responseObserver)
        }
    }

    override fun partyChat(
        request: space.mori.server.proto.Party.PartyChatRequest?,
        responseObserver: StreamObserver<PartyService.PartyChatResponse>?
    ) {
        if (request != null) {
            val uuid = UUID.fromString(request.uuid)
            val message = request.message
            val displayName = request.displayName

            val partyCode = PartyManager.searchPartyID(uuid)

            if (partyCode != -1) {
                partyEventAnnounce(uuid, displayName, partyCode, PartyService.PartyStatus.PARTY_CHAT, message)
            }

            responseObserver?.onNext(PartyService.PartyChatResponse.newBuilder()
                .setSuccess(true).build())
            responseObserver?.onCompleted()
        }
    }

    override fun partyInviteUserSearch(
        request: space.mori.server.proto.Party.PartyInviteUserSearchRequest?,
        responseObserver: StreamObserver<PartyService.PartyInviteUserSearchResponse>?
    ) {
        if (request != null) {
            val inviterUuid = UUID.fromString(request.inviterUuid)
            val inviterDisplayName = request.inviterDisplayName
            val partyCode = request.partyCode
            val username = request.username

            partyInviteStatus[username] = false

            partyEventAnnounce(inviterUuid, inviterDisplayName, partyCode, PartyService.PartyStatus.PARTY_INVITE, username)

            responseObserver?.onNext(PartyService.PartyInviteUserSearchResponse.newBuilder()
                .setSuccess(true).build())
            responseObserver?.onCompleted()

            Thread.sleep(5000L)
            if (partyInviteStatus[username] == false) {
                partyEventAnnounce(inviterUuid, inviterDisplayName, partyCode, PartyService.PartyStatus.PARTY_INVITE_SUCCESS, "false")
            }
        }
    }

    override fun partyInviteProcess(
        request: space.mori.server.proto.Party.PartyInviteProcessRequest?,
        responseObserver: StreamObserver<space.mori.server.proto.Party.PartyInviteProcessResponse>?
    ) {
        if (request != null) {
            val uuid = UUID.fromString(request.uuid)
            val displayName = request.displayName
            val username = request.username
            val partyCode = request.partyCode
            val success = request.success

            partyEventAnnounce(uuid, displayName, partyCode, PartyService.PartyStatus.PARTY_INVITE_SUCCESS,
                success.toString()
            )

            partyInviteStatus[username] = true
        }
    }

    override fun partyInviteAccept(
        request: space.mori.server.proto.Party.PartyInviteAcceptRequest?,
        responseObserver: StreamObserver<space.mori.server.proto.Party.PartyInviteAcceptResponse>?
    ) {
        if (request != null) {
            val uuid = UUID.fromString(request.uuid)
            val displayName = request.displayName
            val partyCode = request.partyCode
            val success = request.success

            if (success && PartyManager.validPartyID(partyCode)) {
                partyEventAnnounce(uuid, displayName, partyCode, PartyService.PartyStatus.PARTY_INVITE_ACCEPT, "true")

                PartyManager.partyJoin(uuid, partyCode)
                partyEventAnnounce(uuid, displayName, partyCode, PartyService.PartyStatus.PARTY_JOIN)
            } else {
                partyEventAnnounce(uuid, displayName, partyCode, PartyService.PartyStatus.PARTY_INVITE_ACCEPT, "false")
            }

            responseObserver?.onNext(PartyService.PartyInviteAcceptResponse.newBuilder()
                .setSuccess(true).build())
            responseObserver?.onCompleted()
        }
    }

    private fun partyEventAnnounce(uuid: UUID, displayName: String, partyCode: Int, partyStatus: PartyService.PartyStatus, message: String = "") {
        val eventContext = PartyService.PartySubscriptionStream.newBuilder()
            .setUuid(uuid.toString()).setDisplayName(displayName).setPartyCode(partyCode)
            .setPartyStatus(partyStatus).setMessage(message)
            .build()

        connectedSession.forEach {
            try {
                it.onNext(eventContext)
            } catch (t: Throwable) {
                Main.logger.error("error!")
                Main.logger.trace(t)

                connectedSession.remove(it)
            }
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

    internal fun validPartyID(partyId: Int): Boolean {
        return PartyList.getOrNull(partyId) != null
    }

    internal fun partyCreate(uuid: UUID): Int {
        return if (uuid in UUIDtoParty) {
            val party = Party(uuid)

            PartyList.add(party)
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