package com.example.getdirectionapi

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable

class AutoSuggestAdapter (context: Context, resource: Int) :
    ArrayAdapter<String>(context, resource), Filterable {

    private var mOriginalData: List<String> = ArrayList()
    private var mFilteredList: List<String> = ArrayList()

    fun setData(list: List<String>) {
        mOriginalData = list
        mFilteredList = list
    }

    override fun getCount(): Int {
        return mFilteredList.size
    }

    override fun getItem(position: Int): String {
        return mFilteredList[position]
    }

    fun getObject(position: Int): String {
        return mFilteredList[position]
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()

                if (constraint != null) {
                    val filteredList = mOriginalData.filter {
                        it.contains(constraint, ignoreCase = true)
                    }

                    filterResults.values = filteredList
                    filterResults.count = filteredList.size
                }

                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null && results.values != null) {
                    @Suppress("UNCHECKED_CAST")
                    mFilteredList = results.values as List<String>
                    notifyDataSetChanged()
                } else {
                    mFilteredList = emptyList() // Set an empty list when there are no results
                    notifyDataSetInvalidated()
                }
            }
        }
    }
}