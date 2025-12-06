package data.repositories

import data.dto.UserRatingsHistoryEntity
import data.dto.UserRatingsHistoryTable
import data.dto.UserSocialCreditsEntity
import data.dto.UsersSocialCreditsTable
import dev.inmo.tgbotapi.types.chat.member.ChatMember
import domain.model.SocialClass
import domain.model.UserSocialCreditsInfo
import domain.repositories.RatingRepository
import domain.utils.Constants
import kotlinx.datetime.*
import kotlinx.datetime.format.DateTimeFormat
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class RatingRepositoryImpl(
    private val dbUrl: String,
    private val dbUser: String,
    private val dbPassword: String
) : RatingRepository {

    init {
        initDatabase()
        createDbSchemas()
    }

    override fun initDatabase() {
        Database.connect(
            url = dbUrl,
            driver = "org.postgresql.Driver",
            user = dbUser,
            password = dbPassword
        )
    }

    override fun createDbSchemas() {
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(
                UsersSocialCreditsTable,
                UserRatingsHistoryTable
            )
        }
    }

    override fun getGroupSocialCreditsList(
        groupId: Long,
        maxRows: Int
    ): List<UserSocialCreditsInfo> = transaction {
        UserSocialCreditsEntity
            .find { UsersSocialCreditsTable.groupId eq groupId }
            .orderBy(Pair(first = UsersSocialCreditsTable.socialCredits, second = SortOrder.DESC))
            .orderBy(Pair(first = UsersSocialCreditsTable.firstName, second = SortOrder.ASC))
            .limit(count = maxRows)
            .map { it.toUserSocialCreditsInfo() }
    }

    override fun getUserSocialCredits(
        groupId: Long,
        userId: Long
    ): UserSocialCreditsInfo? = transaction {
        UserSocialCreditsEntity
            .find { (UsersSocialCreditsTable.groupId eq groupId) and (UsersSocialCreditsTable.userId eq userId) }
            .singleOrNull()?.toUserSocialCreditsInfo()
    }

    @OptIn(ExperimentalTime::class)
    override fun updateUserSocialCredits(
        messageSenderId: Long,
        messageSenderStatus: ChatMember.Status,
        groupId: Long,
        groupTitle: String,
        userId: Long,
        username: String,
        firstName: String,
        socialCreditsChange: Long,
        timeZone: TimeZone,
        dateFormat: DateTimeFormat<LocalDate>
    ): Result<UserSocialCreditsInfo> = try {
        transaction {
            val currentTime = Clock.System.now()
            val currentTimeInMillis = currentTime.toEpochMilliseconds()

            val currentDate = currentTime.toLocalDateTime(
                timeZone = timeZone
            ).date
            val formattedCurrentDate = dateFormat.format(value = currentDate)

            val currentRatingHistory = UserRatingsHistoryEntity.find {
                (UserRatingsHistoryTable.groupId eq groupId) and
                        (UserRatingsHistoryTable.raterUserId eq messageSenderId) and
                        (UserRatingsHistoryTable.targetUserId eq userId)
            }.singleOrNull()

            if (currentRatingHistory != null) { // Previous rating history entity exists
                val isCoolDownOver = when (messageSenderStatus) {
                    ChatMember.Status.Creator, ChatMember.Status.Administrator -> {
                        currentTimeInMillis - currentRatingHistory.modifiedAt > Constants.RATING_COOL_DOWN_IN_MILLIS
                    }
                    ChatMember.Status.Member -> {
                        val modifiedAtDate = LocalDate.parse(
                            input = currentRatingHistory.modifiedAtDate,
                            format = dateFormat
                        )
                        val intervalDays = modifiedAtDate.until(
                            other = currentDate,
                            unit = DateTimeUnit.DAY
                        )
                        intervalDays >= Constants.RATING_COOL_DOWN_FOR_MEMBERS_IN_DAYS
                    }
                    else -> false
                }

                if (!isCoolDownOver) {
                    return@transaction Result.failure(
                        exception = Throwable(message = Constants.THROWABLE_MESSAGE_COOL_DOWN)
                    )
                }

                currentRatingHistory.delete()
            }

            // Create new rating history entity
            UserRatingsHistoryEntity.new {
                this.groupId = groupId
                this.raterUserId = messageSenderId
                this.targetUserId = userId
                this.createdAt = currentTimeInMillis
                this.modifiedAt = currentTimeInMillis
                this.modifiedAtDate = formattedCurrentDate
            }.also { historyEntity ->
                println("User ratings history created: ${historyEntity.toUserRatingsHistory()}")
            }

            val currentUserRating = UserSocialCreditsEntity.find {
                (UsersSocialCreditsTable.groupId eq groupId) and
                        (UsersSocialCreditsTable.userId eq userId)
            }.singleOrNull()

            val updatedSocialCredits = when {
                currentUserRating != null -> { // Previous user rating entity exists
                    (currentUserRating.socialCredits + socialCreditsChange).coerceAtLeast(
                        minimumValue = SocialClass.EXECUTED.credit
                    )
                }
                else -> socialCreditsChange.coerceAtLeast(minimumValue = SocialClass.EXECUTED.credit)
            }

            currentUserRating?.delete()
            val newUserRating = UserSocialCreditsEntity.new {// Previous user rating entity does not exist
                this.groupId = groupId
                this.groupTitle = groupTitle
                this.userId = userId
                this.username = username
                this.firstName = firstName
                this.socialCredits = updatedSocialCredits
                this.createdAt = currentTimeInMillis
                this.modifiedAt = currentTimeInMillis
            }

            Result.success(
                value = newUserRating.toUserSocialCreditsInfo().also {
                    println("User social credits created: $it")
                }
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(exception = Throwable(message = "Update user social credits failed with an exception."))
    }
}