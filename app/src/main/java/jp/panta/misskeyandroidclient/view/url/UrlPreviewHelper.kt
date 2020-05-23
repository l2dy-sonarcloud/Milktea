package jp.panta.misskeyandroidclient.view.url

import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import jp.panta.misskeyandroidclient.model.url.UrlPreview

object UrlPreviewHelper {

    @JvmStatic
    @BindingAdapter("urlPreviewList")
    fun RecyclerView.setUrlPreviewList(urlPreviewList: List<UrlPreview>?){
        urlPreviewList?.let{

        }
        if(urlPreviewList == null){
            this.visibility = View.GONE

        }else{
            this.visibility = View.VISIBLE
            val adapter = UrlPreviewListAdapter()
            adapter.submitList(urlPreviewList)
            this.layoutManager = LinearLayoutManager(this.context)
            this.adapter = adapter
        }

    }

    @JvmStatic
    @BindingAdapter("urlPreviewThumbnailUrl")
    fun ImageView.setUrlPreviewThumbnail(url: String?){
        url?: return
        Glide.with(this)
            .load(url)
            .centerCrop()
            .into(this)
    }

    @JvmStatic
    @BindingAdapter("siteIconUrl")
    fun ImageView.setSiteIcon(url: String?){
        url?: return
        Glide.with(this)
            .load(url)
            .centerCrop()
            .into(this)
    }
}