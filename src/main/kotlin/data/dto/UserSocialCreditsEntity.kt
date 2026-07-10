package data.dto

import domain.model.UserSocialCreditsInfo
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

class UserSocialCreditsEntity(idEntity: EntityID<Long>) : LongEntity(id = idEntity) {
    var groupId by UsersSocialCreditsTable.groupId
    var groupTitle by UsersSocialCreditsTable.groupTitle
    var userId by UsersSocialCreditsTable.userId
    var username by UsersSocialCreditsTable.username
    var firstName by UsersSocialCreditsTable.firstName
    var socialCredits by UsersSocialCreditsTable.socialCredits
    var createdAt by UsersSocialCreditsTable.createdAt
    var modifiedAt by UsersSocialCreditsTable.modifiedAt

    companion object : LongEntityClass<UserSocialCreditsEntity>(table = UsersSocialCreditsTable)

    fun toUserSocialCreditsInfo(): UserSocialCreditsInfo {
        return UserSocialCreditsInfo(
            id = id.value,
            groupId = groupId,
            groupTitle = groupTitle,
            userId = userId,
            username = username,
            firstName = firstName,
            socialCredits = socialCredits,
            createdAt = createdAt,
            modifiedAt = modifiedAt
        )
    }
}