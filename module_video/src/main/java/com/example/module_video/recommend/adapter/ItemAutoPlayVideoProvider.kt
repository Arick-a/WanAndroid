package com.example.module_video.recommend.adapter

import android.view.View
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.provider.BaseItemProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.example.common_base.constants.AConstance
import com.example.common_base.constants.Constants
import com.example.common_base.glide.GlideApp
import com.example.common_base.glide.ProgressImageViewTarget
import com.example.common_base.widget.GlideRoundTransform
import com.example.module_video.R
import com.example.module_video.recommend.bean.OpenRecBean
import com.example.module_video.recommend.bean.VideoInfoBean


/**
 *  @author : zhang.wenqiang
 *  @date : 2021/3/30
 *  description : 自动播放video
 */
class ItemAutoPlayVideoProvider : BaseItemProvider<OpenRecBean>() {

    override val itemViewType: Int
        get() = OpenRecBean.AUTO_PLAY_VIDEO_AD

    override val layoutId: Int
        get() = R.layout.rec_item_auto_play

    override fun convert(helper: BaseViewHolder, item: OpenRecBean) {
        helper.setText(R.id.tv_title, item.data.detail.title)
        helper.setText(R.id.tv_description, item.data.detail.description)

        GlideApp.with(helper.itemView)
            .load(item.data.detail.icon)
            .progress(context)
            .apply(RequestOptions.bitmapTransform(CircleCrop()))
            .into(ProgressImageViewTarget(item.data.detail.icon, helper.getView(R.id.iv_author)))

        GlideApp.with(helper.itemView)
            .load(item.data.detail.imageUrl)
            .progress(context)
            .transform(GlideRoundTransform(context))
            .into(
                ProgressImageViewTarget(
                    item.data.detail.imageUrl,
                    helper.getView(R.id.iv_video_cover)
                )
            )
    }

    override fun onClick(helper: BaseViewHolder, view: View, data: OpenRecBean, position: Int) {
        super.onClick(helper, view, data, position)
        val openRecBean = data.data
        val infoBean = VideoInfoBean()
            .apply {
                videoTitle = openRecBean.header.title
                category = openRecBean.content.data.author.name + " / #" +
                        openRecBean.content.data.category
                video_description = openRecBean.content.data.description
                collectionCount = openRecBean.content.data.consumption.collectionCount
                shareCount = openRecBean.content.data.consumption.shareCount
                avatar = openRecBean.content.data.author.icon
                nickName = openRecBean.content.data.author.name
                user_description = openRecBean.content.data.author.description
                playerUrl = openRecBean.content.data.playUrl
                blurredUrl = openRecBean.content.data.cover.blurred
                videoId = openRecBean.content.data.id
            }

        ARouter.getInstance()
            .build(AConstance.ACTIVITY_URL_VIDEO_PLAY)
            .withParcelable(Constants.VIDEO_PLAY_VIDEO_INFO, infoBean)
            .navigation()
    }
}