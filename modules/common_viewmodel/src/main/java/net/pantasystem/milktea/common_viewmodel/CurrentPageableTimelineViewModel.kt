package net.pantasystem.milktea.common_viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.pantasystem.milktea.model.account.page.Pageable
import javax.inject.Inject

sealed interface SuitableType {
    object Gallery : SuitableType
    data class Channel(val channelId: String) : SuitableType
    object Other : SuitableType

    companion object
}

fun Pageable.suitableType(): SuitableType {
    return when(this) {
        is Pageable.ChannelTimeline -> {
            SuitableType.Channel(this.channelId)
        }
        is Pageable.Gallery -> {
            SuitableType.Gallery
        }
        else -> SuitableType.Other
    }
}


@HiltViewModel
class CurrentPageableTimelineViewModel @Inject constructor(

) : ViewModel() {

    private val _currentType = MutableStateFlow<CurrentPageType>(
        CurrentPageType.Page(Pageable.HomeTimeline()))

    val currentType: StateFlow<CurrentPageType> = _currentType

    fun setCurrentPageable(pageable: Pageable) {
        _currentType.value = CurrentPageType.Page(pageable)
    }

    fun setCurrentPageType(type: CurrentPageType) {
        _currentType.value = type
    }

}

sealed interface CurrentPageType {
    data class Page(val pageable: Pageable) : CurrentPageType
    object Account : CurrentPageType
}