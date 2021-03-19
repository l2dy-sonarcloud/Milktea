package jp.panta.misskeyandroidclient.viewmodel.antenna

import android.util.Log
import androidx.lifecycle.*
import jp.panta.misskeyandroidclient.model.account.Account
import jp.panta.misskeyandroidclient.model.account.page.Pageable
import jp.panta.misskeyandroidclient.api.v12.MisskeyAPIV12
import jp.panta.misskeyandroidclient.api.v12.antenna.AntennaDTO
import jp.panta.misskeyandroidclient.api.v12.antenna.AntennaQuery
import jp.panta.misskeyandroidclient.util.eventbus.EventBus
import jp.panta.misskeyandroidclient.viewmodel.MiCore
import jp.panta.misskeyandroidclient.viewmodel.setting.page.PageableTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AntennaListViewModel (
    val miCore: MiCore
) : ViewModel(){

    @Suppress("UNCHECKED_CAST")
    class Factory(val miCore: MiCore) : ViewModelProvider.Factory{
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return AntennaListViewModel(miCore) as T
        }
    }

    companion object{
        const val TAG = "AntennaViewModel"
    }

    val antennas = MediatorLiveData<List<AntennaDTO>>()

    val editAntennaEvent = EventBus<AntennaDTO>()

    val confirmDeletionAntennaEvent = EventBus<AntennaDTO>()

    val openAntennasTimelineEvent = EventBus<AntennaDTO>()

    val isLoading = MutableLiveData<Boolean>(false)

    private val mPagedAntennaIds = MutableLiveData<Set<String>>()
    val pagedAntennaIds: LiveData<Set<String>> = mPagedAntennaIds

    var account: Account? = null

    init{

        miCore.getCurrentAccount().onEach {
            if(account?.accountId != it?.accountId) {
                loadInit()
                account = it
            }
            mPagedAntennaIds.postValue(
                it?.pages?.mapNotNull { page ->
                    val pageable = page.pageable()
                    if (pageable is Pageable.Antenna) {
                        pageable.antennaId
                    } else {
                        null
                    }
                }?.toSet()?: emptySet()
            )
        }.launchIn(viewModelScope + Dispatchers.IO)


    }

    val deleteResultEvent = EventBus<Boolean>()

    fun loadInit(){
        isLoading.value = true
        val i = miCore.getCurrentAccount().value?.getI(miCore.getEncryption())
            ?: return
        getMisskeyAPI()?.getAntennas(
            AntennaQuery(
                i = i,
                limit = null,
                antennaId = null
            )
        )?.enqueue(object : Callback<List<AntennaDTO>>{
            override fun onResponse(call: Call<List<AntennaDTO>>, response: Response<List<AntennaDTO>>) {
                antennas.postValue(response.body())
                isLoading.postValue(false)
            }

            override fun onFailure(call: Call<List<AntennaDTO>>, t: Throwable) {
                Log.e(TAG, "アンテナ一覧の取得に失敗しました。", t)
                isLoading.postValue(false)
            }
        })
    }

    fun toggleTab(antenna: AntennaDTO?){
        antenna?: return
        val paged = account?.pages?.firstOrNull {

            it.pageParams.antennaId == antenna.id
        }
        if(paged == null){
            miCore.addPageInCurrentAccount(PageableTemplate(account!!).antenna(antenna))
        }else{
            miCore.removePageInCurrentAccount(paged)
        }
    }

    fun confirmDeletionAntenna(antenna: AntennaDTO?){
        antenna?: return
        confirmDeletionAntennaEvent.event = antenna
    }

    fun editAntenna(antenna: AntennaDTO?){
        antenna?: return
        editAntennaEvent.event = antenna
    }

    fun openAntennasTimeline(antenna: AntennaDTO?){
        openAntennasTimelineEvent.event = antenna
    }

    fun deleteAntenna(antenna: AntennaDTO){
        account?.getI(miCore.getEncryption())?.let{ i ->
            getMisskeyAPI()?.deleteAntenna(AntennaQuery(
                i = i,
                antennaId = antenna.id,
                limit = null
            ))?.enqueue(object : Callback<Unit>{
                override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                    deleteResultEvent.event = response.code() in 200 until 300
                    if(response.code() in 200 until 300){
                        loadInit()
                    }
                }
                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    deleteResultEvent.event = false
                }
            })
        }

    }

    private fun getMisskeyAPI(): MisskeyAPIV12?{
        return miCore.getMisskeyAPI(miCore.getCurrentAccount().value!!) as? MisskeyAPIV12
    }
}