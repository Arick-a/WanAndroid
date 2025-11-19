package com.example.module_video.videoDetail

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.example.common_base.base.mvvm.BaseMvvmActivity
import com.example.common_base.constants.AConstance
import com.example.common_base.constants.Constants
import com.example.common_base.util.StatusBarUtil
import com.example.module_video.BR
import com.example.module_video.R
import com.example.module_video.VideoViewModelFactory
import com.example.module_video.databinding.VideoActivityPlayBinding
import com.example.module_video.databinding.VideoItemHeaderViewBinding
import com.example.module_video.recommend.bean.VideoInfoBean
import com.example.module_video.videoDetail.adapter.VideoPlayMultiAdapter
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.utils.OrientationUtils
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer


/**
 * 视频详情页
 */
@Route(path = AConstance.ACTIVITY_URL_VIDEO_PLAY)
class VideoPlayActivity : BaseMvvmActivity<VideoActivityPlayBinding, VideoPlayViewModel>() {
    private lateinit var videoPlayer: StandardGSYVideoPlayer
    private lateinit var orientationUtils: OrientationUtils
    private lateinit var mAdapter: VideoPlayMultiAdapter

    @JvmField
    @Autowired(name = Constants.VIDEO_PLAY_VIDEO_INFO)
    var videoInfo: VideoInfoBean? = null

    override fun initView() {
        ARouter.getInstance().inject(this)
        StatusBarUtil.setDarkMode(this, true)

        initPlayer()
        initRecycler()
        initRefresh()
    }

    private fun initPlayer() {
        videoPlayer = findViewById<View>(R.id.video_player) as StandardGSYVideoPlayer
        videoPlayer.setUp(videoInfo!!.playerUrl, true, videoInfo!!.videoTitle)
        //增加封面
        val imageView = ImageView(this)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        Glide.with(imageView.context)
            .asBitmap()
            .load(videoInfo?.coverImg)
            .into(imageView)
        videoPlayer.thumbImageView = imageView
        //增加title
        videoPlayer.titleTextView.visibility = View.VISIBLE
        //设置返回键
        videoPlayer.backButton.visibility = View.VISIBLE
        //设置旋转
        orientationUtils = OrientationUtils(this, videoPlayer)
        //设置全屏按键功能,这是使用的是选择屏幕，而不是全屏
        videoPlayer.fullscreenButton
            .setOnClickListener {
                orientationUtils.isRotateWithSystem
                orientationUtils.resolveByClick()
            }
        //是否可以滑动调整
        videoPlayer.setIsTouchWiget(true)
        //设置返回按键功能
        videoPlayer.backButton.setOnClickListener { onBackPressed() }
        videoPlayer.startPlayLogic()
    }

    override fun initData() {
        Glide.with(viewDataBinding?.videoPlayBg!!.context)
            .asBitmap()
            .load(videoInfo?.blurredUrl)
            .into(object : SimpleTarget<Bitmap?>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap?>?
                ) {
                    viewDataBinding?.videoPlayBg?.background = BitmapDrawable(resource)
                }
            })
        viewModel.videoId = videoInfo!!.videoId
        viewModel.loadData()

        viewModel.relatedData.observe(this) {
            mAdapter.setNewInstance(it)
        }
    }

    private fun initRefresh() {
        viewDataBinding!!.srlRefresh.apply {
            setOnRefreshListener {
                viewDataBinding!!.srlRefresh.finishRefresh()
            }
            setOnLoadMoreListener {
                viewDataBinding!!.srlRefresh.finishLoadMore()
                viewModel.getRelatedReplies(isLoadMore = true)
            }
            setEnableLoadMore(true)
            setEnableRefresh(true)
        }
    }

    private fun initRecycler() {
        mAdapter =
            VideoPlayMultiAdapter()
        viewDataBinding!!.rvContent.apply {
            layoutManager = LinearLayoutManager(this@VideoPlayActivity)
            adapter = mAdapter
        }
        mAdapter.setHeaderView(generateHeaderView())
    }

    private fun generateHeaderView(): View {
        val binding = DataBindingUtil.inflate<VideoItemHeaderViewBinding>(
            layoutInflater,
            R.layout.video_item_header_view,
            viewDataBinding!!.rvContent,
            false
        )
        binding.videoInfo = videoInfo
        binding.executePendingBindings()
        return binding.root
    }

    override fun getLayoutResId(): Int = R.layout.video_activity_play

    override fun initVariableId(): Int = BR.viewModel

    override fun createViewModel(): VideoPlayViewModel {
        return ViewModelProvider(this, VideoViewModelFactory()).get(VideoPlayViewModel::class.java)
    }

    override fun onPause() {
        super.onPause()
        videoPlayer.onVideoPause()
    }

    override fun onResume() {
        super.onResume()
        videoPlayer.onVideoResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        GSYVideoManager.releaseAllVideos()
        orientationUtils.releaseListener()
    }

    override fun onBackPressed() {
        //先返回正常状态
        if (orientationUtils.screenType == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            videoPlayer.fullscreenButton.performClick()
            return
        }
        //释放所有
        videoPlayer.setVideoAllCallBack(null)
        super.onBackPressed()
    }
}