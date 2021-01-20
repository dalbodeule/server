package space.mori.server.api.services

import io.grpc.stub.StreamObserver
import space.mori.server.api.Main
import space.mori.server.proto.PartyServiceGrpc
import java.util.UUID
import space.mori.server.proto.Party as PartyService

class PartyService : PartyServiceGrpc.PartyServiceImplBase() {
    private val connectedSession: MutableList<StreamObserver<PartyService.PartySubscriptionStream>> = mutableListOf()

    override fun partyCreate(
        request: PartyService.PartyCreateRequest?,
        responseObserver: StreamObserver<PartyService.PartyCreateResponse>?
    ) {
        if (request != null) {
            val uuid = UUID.fromString(request.uuid)

            val result = PartyManager.partyCreate(uuid)

            val response = PartyService.PartyCreateResponse.newBuilder()

            if (result == -1) {
                response.success = false
                response.partyCode = -1
            } else {
                response.success = true
                response.partyCode = result

                partyEventAnnounce(uuid, result, PartyService.PartyStatus.PARTY_CREATE)
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

            val result = PartyManager.partyJoin(uuid, partyCode)

            if (result) partyEventAnnounce(uuid, partyCode, PartyService.PartyStatus.PARTY_JOIN)

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

            val result = PartyManager.partyLeave(uuid)

            if (result) partyEventAnnounce(uuid, partyCode, PartyService.PartyStatus.PARTY_LEAVE)

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

            val result = PartyManager.partyRemove(uuid)

            if (result) partyEventAnnounce(uuid, partyCode, PartyService.PartyStatus.PARTY_REMOVE)

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

    private fun partyEventAnnounce(uuid: UUID, partyCode: Int, partyStatus: PartyService.PartyStatus) {
        val eventContext = PartyService.PartySubscriptionStream.newBuilder()
            .setUuid(uuid.toString()).setPartyCode(partyCode).setPartyStatus(partyStatus)
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
    val partyUser: MutableList<UUID> = mutableListOf()
) {
    internal fun addUser(uuid: UUID): Boolean {
        return if (partyUser.size >= 3) {
            false
        } else {
            partyUser.add(uuid)
            true
        }
    }

    internal fun removeUser(uuid: UUID): Boolean {
        return if (uuid in partyUser) {
            partyUser.remove(uuid)
            true
        } else {
            false
        }
    }
}