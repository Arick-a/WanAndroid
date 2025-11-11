package com.example.module_usercenter.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.module_usercenter.R
import com.example.module_usercenter.adapter.CommentAdapter

class ListFragment : Fragment() {

    companion object {
        fun newInstance(title: String) = ListFragment().apply {
            arguments = Bundle().apply { putString("title", title) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.recycler_view)
        rv.layoutManager = LinearLayoutManager(requireContext())
        val data = List(30) { "${arguments?.getString("title")} item $it" }
        rv.adapter = CommentAdapter(data)
    }
}
