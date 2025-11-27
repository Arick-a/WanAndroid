package com.example.module_usercenter.activity

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.example.common_base.base.BaseActivity
import com.example.common_base.util.StatusBarUtil
import com.example.module_usercenter.R
import com.example.module_usercenter.adapter.SpacingDecoration
import com.example.module_usercenter.adapter.StaggeredAdapter
import com.example.module_usercenter.adapter.dp2px
import com.example.module_usercenter.bean.StaggeredItem

@Route(path = "/user/staggeredDemo")
class StaggeredGridLayoutActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private val adapter = StaggeredAdapter()
    private var isLoading = false // 防止重复加载
    private var currentPage = 0 // 当前页码
    private val pageSize = 20 // 每页加载的数量
    private var currentItemId = 0 // 用于生成唯一 id


    override fun getLayoutResId(): Int = R.layout.activity_staggered

    override fun initView() {
        StatusBarUtil.setDarkMode(this, true)

        recyclerView = findViewById(R.id.rvList)

        val layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL).apply {
                gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
            }

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(SpacingDecoration(dp2px(this, 8)))
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                // 仅在停止滚动时检查加载更多，或根据需求在滚动时检查
                // if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                //     checkLoadMore(layoutManager)
                // }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // 在滚动过程中检查是否需要加载更多
                if (dy > 0) { // 仅在向下滚动时检查
                    checkLoadMore(layoutManager)
                }
            }
        })
    }

    private fun checkLoadMore(layoutManager: StaggeredGridLayoutManager) {
        if (isLoading) return

        val lastVisibleItems = layoutManager.findLastVisibleItemPositions(null)
        val lastItemIndex = adapter.itemCount - 1

        // 瀑布流判断到达底部：当所有列的最后一个可见项索引都非常接近列表末尾时触发加载
        // 这里的阈值设置为倒数第 5 个 item
        if (lastVisibleItems.any { it >= lastItemIndex - 5 }) {
            // Log.d("LoadMore", "Triggering Load More. Last visible: ${lastVisibleItems.joinToString()}")
            loadMoreData()
        }
    }

    private fun loadMoreData() {
        isLoading = true
        currentPage++

        // 模拟网络延迟
        recyclerView.postDelayed({
            val newItems = generateItems(pageSize)

            // 获取当前列表，追加新数据
            val currentList = adapter.currentList.toMutableList()
            currentList.addAll(newItems)

            // 预计算高度并提交列表
            adapter.submitListWithPreMeasure(currentList, this) {
                isLoading = false
            }

            // Log.d("LoadMore", "Page $currentPage loaded. Total items: ${currentList.size}")

        }, 800) // 模拟 800ms 网络延迟
    }

    private fun generateItems(count: Int): List<StaggeredItem> {
        val newItems = MutableList(count) {
            val id = currentItemId++
            if (id % 2 == 0) {
                StaggeredItem(
                    id = id,
                    type = StaggeredItem.TYPE_TEXT,
                    title = randomTitle(),
                    content = randomText()
                )
            } else {
                StaggeredItem(
                    id = id,
                    type = StaggeredItem.TYPE_IMAGE,
                    imageRes = R.drawable.ic_launcher_foreground
                )
            }
        }
        return newItems
    }

    override fun initData() {
        // 生成一些测试数据（文字 + 图片）
        val items = MutableList(20) {
            if (it % 2 == 0) {
                StaggeredItem(
                    id = it,
                    type = StaggeredItem.TYPE_TEXT,
                    title = randomTitle(), // 使用新方法生成标题
                    content = randomText() // 使用原方法生成内容
                )
            } else {
                StaggeredItem(
                    id = it,
                    type = StaggeredItem.TYPE_IMAGE,
                    imageRes = R.drawable.ic_launcher_foreground
                )
            }
        }

        // 预计算高度
        adapter.submitListWithPreMeasure(items, this)
    }

    private fun randomTitle(): String {
        val titles = listOf(
            "热门推荐", "生活妙招", "看到黑子酸看到黑子酸看到黑子酸看到黑子酸看到黑子酸看到黑子酸看到黑子酸看到黑子酸看到黑子酸看到黑子酸看到黑子酸看到黑子酸看到黑子酸看到黑子酸看到黑子酸看到黑子酸看到黑子酸看到黑子酸", "旅行日记", "美食探索"
        )
        return titles.random()
    }

    private fun randomText(): String {
        val words = listOf(
            "风吹草地见牛羊", "人生若只如初见", "代码如诗，简洁优雅",
            "Android 开发", "瀑布流布局", "测试文本", "这是一个很长的文本用于测试"
        )
        return (1..(2..12).random()).joinToString(" ") { words.random() }
    }
}
