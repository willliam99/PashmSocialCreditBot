package domain.repositories

import dev.inmo.tgbotapi.types.chat.member.ChatMember
import domain.model.UserSocialCreditsInfo
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DateTimeFormat

interface RatingRepository {

    fun initDatabase()

    fun createDbSchemas()

    fun getGroupSocialCreditsList(
        groupId: Long,
        maxRows: Int = 50
    ): List<UserSocialCreditsInfo>

    fun getUserSocialCredits(
        groupId: Long,
        userId: Long
    ): UserSocialCreditsInfo?

    fun updateUserSocialCredits(
        messageSenderId: Long,
        messageSenderStatus: ChatMember.Status,
        groupId: Long,
        groupTitle: String,
        userId: Long,
        username: String,
        firstName: String,
        socialCreditsChange: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        dateFormat: DateTimeFormat<LocalDate> = LocalDate.Format {
            /**
             * DateTime Format: yyyy-mm-dd
             * Sample: 2024-09-24
             */
            date(format = LocalDate.Formats.ISO)
        }
    ): Result<UserSocialCreditsInfo>
}