package com.example.module_video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.module_video.data.DefOpenEyeRepository
import com.example.module_video.recommend.OpenEyeViewModel
import com.example.module_video.videoDetail.VideoPlayViewModel

/**
 *  @author : zhang.wenqiang
 *  @date : 2020/12/1
 *  description :
 */
class VideoViewModelFactory : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>) = with(modelClass) {
        when {
            isAssignableFrom(OpenEyeViewModel::class.java) ->
                OpenEyeViewModel(DefOpenEyeRepository())
            isAssignableFrom(VideoPlayViewModel::class.java) ->
                VideoPlayViewModel(DefOpenEyeRepository())
            else ->
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    } as T
}