package net.pantasystem.milktea.note

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import net.pantasystem.milktea.app_store.account.AccountStore
import net.pantasystem.milktea.common.text.LevenshteinDistance
import net.pantasystem.milktea.common_android.resource.StringSource
import net.pantasystem.milktea.model.account.Account
import net.pantasystem.milktea.model.emoji.CustomEmojiRepository
import net.pantasystem.milktea.model.emoji.Emoji
import net.pantasystem.milktea.model.emoji.UserEmojiConfig
import net.pantasystem.milktea.model.emoji.UserEmojiConfigRepository
import net.pantasystem.milktea.model.notes.reaction.LegacyReaction
import net.pantasystem.milktea.model.notes.reaction.history.ReactionHistory
import net.pantasystem.milktea.model.notes.reaction.history.ReactionHistoryCount
import net.pantasystem.milktea.model.notes.reaction.history.ReactionHistoryRepository


class EmojiPickerUiStateService(
    accountStore: AccountStore,
    private val customEmojiRepository: CustomEmojiRepository,
    private val reactionHistoryRepository: ReactionHistoryRepository,
    private val userEmojiConfigRepository: UserEmojiConfigRepository,
    coroutineScope: CoroutineScope,
) {


    @OptIn(ExperimentalCoroutinesApi::class)
    private val emojis = accountStore.observeCurrentAccount.filterNotNull().flatMapLatest { ac ->
        customEmojiRepository.observeBy(ac.getHost())
    }.flowOn(Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val reactionCount =
        accountStore.observeCurrentAccount.filterNotNull().flatMapLatest { ac ->
            reactionHistoryRepository.observeSumReactions(ac.normalizedInstanceDomain)
        }.flowOn(Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val userSetting =
        accountStore.observeCurrentAccount.filterNotNull().flatMapLatest { ac ->
            userEmojiConfigRepository.observeByInstanceDomain(ac.normalizedInstanceDomain)
        }

    private val reactions = combine(reactionCount, userSetting) { counts, settings ->
        Reactions(
            settings,
            counts,
        )
    }.stateIn(
        coroutineScope,
        SharingStarted.WhileSubscribed(5_000),
        Reactions(emptyList(), emptyList())
    )

    private val baseInfo = combine(accountStore.observeCurrentAccount, emojis) { account, emojis ->
        BaseInfo(account, emojis)
    }.stateIn(
        coroutineScope,
        SharingStarted.WhileSubscribed(5_000),
        BaseInfo(null, emptyList())
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val recentlyUsedReactions =
        accountStore.observeCurrentAccount.filterNotNull().flatMapLatest {
            reactionHistoryRepository.observeRecentlyUsedBy(it.normalizedInstanceDomain, limit = 20)
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val searchWord = MutableStateFlow("")


    // 検索時の候補
    val uiState: StateFlow<EmojiPickerUiState> = combine(
        searchWord,
        baseInfo,
        reactions,
        recentlyUsedReactions
    ) { word, (ac, emojis), (settings, counts), recentlyUsed ->
        EmojiPickerUiState(
            keyword = word,
            account = ac,
            customEmojis = emojis,
            reactionHistoryCounts = counts,
            userSettingReactions = settings,
            recentlyUsedReactions = recentlyUsed
        )
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        EmojiPickerUiState(
            "",
            null, emptyList(),
            emptyList(),
            emptyList(),
            emptyList()
        )
    )

    val tabLabels = uiState.map { uiState ->
        uiState.segments.map {
            it.label
        }
    }.distinctUntilChanged()
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

data class EmojiPickerUiState(
    val keyword: String,
    val account: Account?,
    val customEmojis: List<Emoji>,
    val reactionHistoryCounts: List<ReactionHistoryCount>,
    val userSettingReactions: List<UserEmojiConfig>,
    val recentlyUsedReactions: List<ReactionHistory>,
) {

    val isSearchMode = keyword.isNotBlank()

    private val frequencyUsedReactionsV2: List<EmojiType> by lazy {
        reactionHistoryCounts.map {
            it.reaction
        }.mapNotNull { reaction ->
            EmojiType.from(customEmojis, reaction)
        }.distinct()
    }

    val all: List<EmojiType> by lazy {
        LegacyReaction.defaultReaction.map {
            EmojiType.Legacy(it)
        } + (customEmojis.map {
            EmojiType.CustomEmoji(it)
        })
    }


    private val userSettingEmojis: List<EmojiType> by lazy {
        userSettingReactions.mapNotNull { setting ->
            EmojiType.from(customEmojis, setting.reaction)
        }.ifEmpty {
            LegacyReaction.defaultReaction.map {
                EmojiType.Legacy(it)
            }
        }
    }

    private val otherEmojis = customEmojis.filter {
        it.category == null
    }.map {
        EmojiType.CustomEmoji(it)
    }

    private fun getCategoryBy(category: String): List<EmojiType> {
        return customEmojis.filter {
            it.category == category

        }.map {
            EmojiType.CustomEmoji(it)
        }
    }

    private val categories = customEmojis.filterNot {
        it.category.isNullOrBlank()
    }.mapNotNull {
        it.category
    }.distinct()

    private val recentlyUsed = recentlyUsedReactions.mapNotNull {
        EmojiType.from(customEmojis, it.reaction)
    }

    val segments = listOfNotNull(
        SegmentType.UserCustom(userSettingEmojis),
        SegmentType.OftenUse(frequencyUsedReactionsV2),
        SegmentType.RecentlyUsed(recentlyUsed),
        otherEmojis.let {
            SegmentType.OtherCategory(it)
        },
    ) + categories.map {
        SegmentType.Category(
            it,
            getCategoryBy(it)
        )
    }

    val searchFilteredEmojis = customEmojis.filterEmojiBy(keyword).map {
        EmojiType.CustomEmoji(it)
    }.sortedBy {
        LevenshteinDistance(it.emoji.name, keyword)
    }
}

sealed interface SegmentType {
    val label: StringSource
    val emojis: List<EmojiType>

    data class Category(val name: String, override val emojis: List<EmojiType>) : SegmentType {
        override val label: StringSource
            get() = StringSource.invoke(name)
    }

    data class UserCustom(override val emojis: List<EmojiType>) : SegmentType {
        override val label: StringSource
            get() = StringSource.invoke(R.string.user)
    }

    data class OftenUse(override val emojis: List<EmojiType>) : SegmentType {
        override val label: StringSource
            get() = StringSource.invoke(R.string.often_use)
    }

    data class OtherCategory(override val emojis: List<EmojiType>) : SegmentType {
        override val label: StringSource
            get() = StringSource.invoke(R.string.other)
    }
    data class RecentlyUsed(override val emojis: List<EmojiType>) : SegmentType {
        override val label: StringSource
            get() = StringSource.invoke(R.string.recently_used)
    }
}

sealed interface EmojiType {
    data class Legacy(val type: String) : EmojiType
    data class CustomEmoji(val emoji: Emoji) : EmojiType
    data class UtfEmoji(val code: String) : EmojiType
    companion object
}

fun EmojiType.toTextReaction(): String {
    return when (val type = this) {
        is EmojiType.CustomEmoji -> {
            ":${type.emoji.name}:"
        }
        is EmojiType.Legacy -> {
            type.type
        }
        is EmojiType.UtfEmoji -> {
            type.code
        }
    }
}


fun EmojiType.Companion.from(emojis: List<Emoji>?, reaction: String): EmojiType? {
    return if (reaction.codePointCount(0, reaction.length) == 1) {
        EmojiType.UtfEmoji(reaction)
    } else if (reaction.startsWith(":") && reaction.endsWith(":") && reaction.contains(
            "@"
        )
    ) {
        val nameOnly = reaction.replace(":", "")
        (emojis?.firstOrNull {
            it.name == nameOnly
        } ?: (nameOnly.split("@")[0]).let { name ->
            emojis?.firstOrNull {
                it.name == name
            }
        })?.let {
            EmojiType.CustomEmoji(it)
        }
    } else if (LegacyReaction.reactionMap[reaction] != null) {
        EmojiType.Legacy(reaction)
    } else {
        emojis?.firstOrNull {
            it.name == reaction.replace(":", "")
        }?.let {
            EmojiType.CustomEmoji(it)
        }
    }
}

fun List<Emoji>.filterEmojiBy(word: String): List<Emoji> {
    val w = word.replace(":", "")
    return filter { emoji ->
        emoji.name.contains(w)
                || emoji.aliases?.any { alias ->
            alias.startsWith(w)
        } ?: false
    }
}

private data class Reactions(
    val userSettings: List<UserEmojiConfig>,
    val reactionHistoryCounts: List<ReactionHistoryCount>,
)

private data class BaseInfo(
    val account: Account?,
    val emojis: List<Emoji>,
)