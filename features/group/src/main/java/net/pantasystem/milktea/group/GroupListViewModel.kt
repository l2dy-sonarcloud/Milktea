package net.pantasystem.milktea.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import net.pantasystem.milktea.common.ResultState
import net.pantasystem.milktea.common.StateContent
import net.pantasystem.milktea.common.asLoadingStateFlow
import net.pantasystem.milktea.app_store.account.AccountStore
import net.pantasystem.milktea.model.group.Group
import net.pantasystem.milktea.model.group.GroupDataSource
import net.pantasystem.milktea.model.group.GroupRepository
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GroupListViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val accountStore: AccountStore,
    private val groupDataSource: GroupDataSource,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val joinedGroups = accountStore.observeCurrentAccount.filterNotNull().flatMapLatest { account ->
        groupDataSource.observeJoinedGroups(account.accountId)
    }.flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val ownedGroups = accountStore.observeCurrentAccount.filterNotNull().flatMapLatest { account ->
        groupDataSource.observeOwnedGroups(account.accountId)
    }.flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val syncEvents = MutableSharedFlow<UUID>()

    private val ownedGroupSyncState = syncEvents.flatMapLatest {
        accountStore.observeCurrentAccount
    }.filterNotNull().flatMapLatest {
        suspend {
            groupRepository.syncByOwned(it.accountId)
        }.asLoadingStateFlow()
    }.flowOn(Dispatchers.IO).stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        ResultState.Loading(StateContent.NotExist())
    )

    private val joinedGroupSyncState = syncEvents.flatMapLatest {
        accountStore.observeCurrentAccount
    }.filterNotNull().flatMapLatest {
        suspend {
            groupRepository.syncByJoined(it.accountId)
        }.asLoadingStateFlow()
    }.flowOn(Dispatchers.IO).stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        ResultState.Loading(StateContent.NotExist())
    )

    val uiState = combine(
        ownedGroups,
        joinedGroups,
        ownedGroupSyncState,
        joinedGroupSyncState
    ) { ownedGroups, joinedGroups, ownedSyncState, joinedSyncState ->
        GroupListUiState(
            ownedGroups = ownedGroups,
            joinedGroups = joinedGroups,
            syncJoinedGroupsState = joinedSyncState,
            syncOwnedGroupsState = ownedSyncState
        )
    }.flowOn(Dispatchers.IO).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(1000),
        GroupListUiState(
            emptyList(),
            emptyList(),
            ResultState.Loading(StateContent.NotExist()),
            ResultState.Loading(StateContent.NotExist())
        )
    )

    init {
        sync()
    }

    fun sync() {
        syncEvents.tryEmit(UUID.randomUUID())
    }

}

data class GroupListUiState(
    val joinedGroups: List<Group>,
    val ownedGroups: List<Group>,
    val syncJoinedGroupsState: ResultState<List<Group>>,
    val syncOwnedGroupsState: ResultState<List<Group>>,
)