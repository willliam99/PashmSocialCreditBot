package data.dto

import domain.model.UserRatingsHistory
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

class UserRatingsHistoryEntity(idEntity: EntityID<Long>) : LongEntity(id = idEntity) {
    var groupId by UserRatingsHistoryTable.groupId
    var raterUserId by UserRatingsHistoryTable.raterUserId
    var targetUserId by UserRatingsHistoryTable.targetUserId
    var createdAt by UserRatingsHistoryTable.createdAt
    var modifiedAt by UserRatingsHistoryTable.modifiedAt
    var modifiedAtDate by UserRatingsHistoryTable.modifiedAtDate

    companion object : LongEntityClass<UserRatingsHistoryEntity>(table = UserRatingsHistoryTable)

    fun toUserRatingsHistory(): UserRatingsHistory {
        return UserRatingsHistory(
            id = id.value,
            groupId = groupId,
            raterUserId = raterUserId,
            targetUserId = targetUserId,
            createdAt = createdAt,
            modifiedAt = modifiedAt,
            modifiedAtDate = modifiedAtDate
        )
    }
}