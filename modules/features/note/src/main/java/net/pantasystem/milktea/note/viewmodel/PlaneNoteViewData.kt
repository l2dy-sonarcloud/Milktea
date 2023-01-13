package net.pantasystem.milktea.note.viewmodel


import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import net.pantasystem.milktea.app_store.notes.NoteTranslationStore
import net.pantasystem.milktea.common.ResultState
import net.pantasystem.milktea.common_android.mfm.MFMParser
import net.pantasystem.milktea.model.account.Account
import net.pantasystem.milktea.model.emoji.Emoji
import net.pantasystem.milktea.model.file.AboutMediaType
import net.pantasystem.milktea.model.file.AppFile
import net.pantasystem.milktea.model.file.FilePreviewSource
import net.pantasystem.milktea.model.notes.*
import net.pantasystem.milktea.model.notes.poll.Poll
import net.pantasystem.milktea.model.url.UrlPreview
import net.pantasystem.milktea.model.url.UrlPreviewLoadTask
import net.pantasystem.milktea.model.user.User
import net.pantasystem.milktea.note.media.viewmodel.MediaViewData

open class PlaneNoteViewData(
    val note: NoteRelation,
    val account: Account,
    noteCaptureAPIAdapter: NoteCaptureAPIAdapter,
    private val noteTranslationStore: NoteTranslationStore,
    private val instanceEmojis: List<Emoji>,
) : NoteViewData {


    val id = note.note.id

    override fun getRequestId(): String {
        return id.noteId
    }

    val toShowNote: NoteRelation
        get() {
            return if (note.note.isRenote() && !note.note.hasContent()) {
                note.renote ?: note
            } else {
                note
            }
        }


    val isRenotedByMe = !note.note.hasContent() && note.user.id.id == account.remoteId

    val statusMessage: String?
        get() {
            if (note.reply != null) {
                //reply
                return "${note.user.displayUserName}が返信しました"
            } else if (note.note.renoteId == null && (note.note.text != null || note.files != null)) {
                //Note
                return null
            } else if (note.note.renoteId != null && note.note.text == null && note.files.isNullOrEmpty()) {
                //reNote
                return "${note.user.displayUserName}がリノートしました"

            } else if (note.note.renoteId != null && (note.note.text != null || note.files != null)) {
                //quote
                //"${note.user.name}が引用リノートしました"
                return null
            } else {
                return null
            }
        }

    val userId: User.Id
        get() = toShowNote.user.id

    val name: String
        get() = toShowNote.user.displayName

    val userName: String = toShowNote.user.displayUserName

    val avatarUrl = toShowNote.user.avatarUrl

    val cw = toShowNote.note.cw
    val cwNode = MFMParser.parse(
        toShowNote.note.cw, (toShowNote.note.emojis ?: emptyList()) + instanceEmojis,
        userHost = toShowNote.user
            .host,
        accountHost = account.getHost()
    )

    //true　折り畳み
    val text = toShowNote.note.text

    val contentFolding = MutableLiveData(cw != null)
    val contentFoldingStatusMessage: LiveData<String> = Transformations.map(contentFolding) {
        if (it) "もっと見る: ${text?.length}文字" else "隠す"
    }


    val textNode = MFMParser.parse(
        toShowNote.note.text, (toShowNote.note.emojis ?: emptyList()) + instanceEmojis,
        userHost = toShowNote.user
            .host,
        accountHost = account.getHost()
    )

    val translateState: LiveData<ResultState<Translation?>?> =
        this.noteTranslationStore.state(toShowNote.note.id).asLiveData()

    var emojis = toShowNote.note.emojis ?: emptyList()

    val emojiMap = HashMap<String, Emoji>(toShowNote.note.emojis?.associate {
        it.name to it
    } ?: mapOf())

    private val previewableFiles = toShowNote.files?.map {
        FilePreviewSource.Remote(AppFile.Remote(it.id), it)
    }?.filter {
        it.aboutMediaType == AboutMediaType.IMAGE || it.aboutMediaType == AboutMediaType.VIDEO
    }?: emptyList()
    val media = MediaViewData(previewableFiles)

    val isOnlyVisibleRenoteStatusMessage = MutableLiveData<Boolean>(false)


    val urlPreviewList = MutableLiveData<List<UrlPreview>>()

    val previews = MediatorLiveData<List<Preview>>().apply {
        val otherFiles = toShowNote.files?.map { file ->
            FilePreviewSource.Remote(AppFile.Remote(file.id), file)
        }?.filterNot { fp ->
            fp.aboutMediaType == AboutMediaType.IMAGE || fp.aboutMediaType == AboutMediaType.VIDEO
        }?.map { file ->
            Preview.FileWrapper(file)
        }

        postValue(otherFiles)
        this.addSource(urlPreviewList) {
            val urlPreviews = it?.map { url ->
                Preview.UrlWrapper(url)
            } ?: emptyList()
            postValue((otherFiles ?: emptyList()) + urlPreviews)

        }
    }

    //var replyCount: String? = if(toShowNote.replyCount > 0) toShowNote.replyCount.toString() else null
    val replyCount = MutableLiveData(toShowNote.note.repliesCount)

    val reNoteCount: String?
        get() = if (toShowNote.note.renoteCount > 0) toShowNote.note.renoteCount.toString() else null
    val renoteCount = MutableLiveData(toShowNote.note.renoteCount)

    val canRenote =
        toShowNote.note.canRenote(User.Id(accountId = account.accountId, id = account.remoteId))

    val reactionCounts = MutableLiveData(toShowNote.note.reactionCounts)

    val reactionCount = Transformations.map(reactionCounts) {
        var sum = 0
        it?.forEach { count ->
            sum += count.count
        }
        return@map sum
    }

    val myReaction = MutableLiveData<String?>(toShowNote.note.myReaction)

    val poll = MutableLiveData<Poll?>(toShowNote.note.poll)

    //reNote先
    val subNote: NoteRelation? = toShowNote.renote

    val subNoteAvatarUrl = subNote?.user?.avatarUrl
    private val subNoteText = subNote?.note?.text
    val subNoteTextNode = MFMParser.parse(
        subNote?.note?.text,
        (subNote?.note?.emojis ?: emptyList()) + instanceEmojis,
        accountHost = account.getHost(),
        userHost = subNote?.user?.host
    )

    val subCw = subNote?.note?.cw
    val subCwNode = MFMParser.parse(
        subNote?.note?.cw,
        (subNote?.note?.emojis?: emptyList()) + instanceEmojis,
        accountHost = account.getHost(),
        userHost = subNote?.user?.host
    )

    //true　折り畳み
    val subContentFolding = MutableLiveData(subCw != null)

    val subContentFoldingStatusMessage = Transformations.map(subContentFolding) { isFolding ->
        CwTextGenerator(subNote, isFolding)
    }
    val subNoteFiles = subNote?.files ?: emptyList()
    val subNoteMedia = MediaViewData(subNote?.files?.map {
        FilePreviewSource.Remote(AppFile.Remote(it.id), it)
    } ?: emptyList())


    fun changeContentFolding() {
        val isFolding = contentFolding.value ?: return
        contentFolding.value = !isFolding
    }

    fun changeSubContentFolding() {
        val isFolding = subContentFolding.value ?: return
        subContentFolding.value = !isFolding
    }


    val urlPreviewLoadTaskCallback = object : UrlPreviewLoadTask.Callback {
        override fun accept(list: List<UrlPreview>) {
            urlPreviewList.postValue(list)
        }
    }

    fun update(note: Note) {
        require(toShowNote.note.id == note.id) {
            "更新として渡されたNote.Idと現在のIdが一致しません。"
        }
        emojiMap.putAll(note.emojis?.map {
            it.name to it
        } ?: emptyList())
        emojis = emojiMap.values.toList() + instanceEmojis
        renoteCount.postValue(note.renoteCount)

        myReaction.postValue(note.myReaction)
        reactionCounts.postValue(note.reactionCounts)
        note.poll?.let {
            poll.postValue(it)
        }
    }


    val eventFlow = noteCaptureAPIAdapter.capture(toShowNote.note.id).onEach {
        if (it is NoteDataSource.Event.Updated) {
            update(it.note)
        }
    }.catch { e ->
        Log.d("PlaneNoteViewData", "error", e)
    }

    var job: Job? = null

    // NOTE: (Panta) cwの時点で大半が隠されるので折りたたむ必要はない
    // NOTE: (Panta) cwを折りたたんでしまうとcw展開後に自動的に折りたたまれてしまって二度手間になる可能性がある。
    val expanded = MutableLiveData<Boolean>(cw != null)


    init {
        require(toShowNote.note.id != subNote?.note?.id)
    }

    fun expand() {
        Log.d("PlaneNoteViewData", "expand")
        expanded.value = true
    }

}