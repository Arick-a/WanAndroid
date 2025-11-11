package com.example.module_usercenter.bean

data class TimelineNode(
    val title: String,
    val time: String?,
    val isCurrent: Boolean = false,
    val isDone: Boolean = false
)