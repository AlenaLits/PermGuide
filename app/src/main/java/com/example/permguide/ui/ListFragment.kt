package com.example.permguide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.permguide.R
import com.example.permguide.adapter.AttractionAdapter
import com.example.permguide.model.Attraction
import com.example.permguide.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.appcompat.widget.SearchView
import com.google.android.material.chip.ChipGroup

class ListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var attractionsList = listOf<Attraction>()
    private var adapter: AttractionAdapter? = null
    private var selectedCategoryId: Int? = null
    private var currentSearchQuery: String = ""
    private var selectedCategoryIds = mutableSetOf<Int>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_list, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val searchView = view.findViewById<SearchView>(R.id.searchView)
        val searchText = searchView.findViewById<android.widget.EditText>(androidx.appcompat.R.id.search_src_text)
        searchText.setTextColor(android.graphics.Color.WHITE)
        searchText.setHintTextColor(android.graphics.Color.GRAY)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText ?: ""
                applyCombinedFilters() // Вызываем общую фильтрацию
                return true
            }
        })
        // Находим ChipGroup и сам чип "Все"
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroup)
        val chipAll = view.findViewById<com.google.android.material.chip.Chip>(R.id.chipAll)

// Список всех "категорийных" чипов (кроме "Все")
        val categoryChips = listOf(
            view.findViewById<com.google.android.material.chip.Chip>(R.id.chipMuseums),
            view.findViewById<com.google.android.material.chip.Chip>(R.id.chipParks),
            view.findViewById<com.google.android.material.chip.Chip>(R.id.chipStatues),
            view.findViewById<com.google.android.material.chip.Chip>(R.id.chipArchitecture),
            view.findViewById<com.google.android.material.chip.Chip>(R.id.chipTheaters)
        )

// ЛОГИКА ДЛЯ ЧИПА "ВСЕ"
        chipAll.setOnClickListener {
            // Если нажали "Все", принудительно выключаем все остальные
            categoryChips.forEach { it.isChecked = false }
            chipAll.isChecked = true // "Все" всегда остается активным при нажатии
            selectedCategoryIds.clear()
            applyCombinedFilters()
        }

// ЛОГИКА ДЛЯ ОСТАЛЬНЫХ КАТЕГОРИЙ
        categoryChips.forEach { chip ->
            chip.setOnClickListener {
                if (chip.isChecked) {
                    // Если включили какую-то категорию — выключаем "Все"
                    chipAll.isChecked = false
                }

                // Обновляем список выбранных ID
                updateSelectedIds(categoryChips)

                // Если вдруг все категории выключены — автоматически включаем "Все"
                if (selectedCategoryIds.isEmpty()) {
                    chipAll.isChecked = true
                }

                applyCombinedFilters()
            }
        }

        fetchAttractions()
        return view
    }
    private fun updateSelectedIds(chips: List<com.google.android.material.chip.Chip>) {
        selectedCategoryIds.clear()
        chips.forEach { chip ->
            if (chip.isChecked) {
                when (chip.id) {
                    R.id.chipMuseums -> selectedCategoryIds.add(1)
                    R.id.chipParks -> selectedCategoryIds.add(2)
                    R.id.chipStatues -> selectedCategoryIds.add(3)
                    R.id.chipArchitecture -> selectedCategoryIds.add(4)
                    R.id.chipTheaters -> selectedCategoryIds.add(5)
                }
            }
        }
    }
    private fun applyCombinedFilters() {
        // Берем базовый список (где уже нет ID 13)
        var filtered = attractionsList

        // Шаг 1: Фильтруем по категории (если она выбрана)
        if (selectedCategoryIds.isNotEmpty()) {
            filtered = filtered.filter { attraction ->
                selectedCategoryIds.contains(attraction.categoryAttraction)
            }
        }

        // Шаг 2: Фильтруем по поисковому запросу (если он не пустой)
        if (currentSearchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.nameAttraction.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        // Выводим логи для проверки
        android.util.Log.d("FILTER_TAG", "Query: $currentSearchQuery, Category: $selectedCategoryId, Found: ${filtered.size}")

        // Обновляем адаптер
        adapter?.updateList(filtered)
    }
    private fun fetchAttractions() {
        RetrofitClient.instance.getAttractions().enqueue(object : Callback<List<Attraction>> {
            override fun onResponse(call: Call<List<Attraction>>, response: Response<List<Attraction>>) {
                if (response.isSuccessful) {
                    val fullList = response.body() ?: emptyList()

                    // 1. Убираем город (ID 13)
                    // 2. Оставляем только уникальные достопримечательности по их ID
                    attractionsList = fullList
                        .filter { it.idAttraction != 13 }
                        .distinctBy { it.idAttraction } // ВОТ ЭТА СТРОЧКА УБЕРЕТ ДУБЛИ

                    adapter = AttractionAdapter(attractionsList) { attraction ->
                        val fragment = AttractionDetailFragment()
                        val bundle = Bundle().apply {
                            putInt("id", attraction.idAttraction)
                            putString("name", attraction.nameAttraction)
                            putString("description", attraction.descriptionAttraction)
                        }

                        fragment.arguments = bundle

                        parentFragmentManager.beginTransaction()
                            .replace(R.id.container, fragment)
                            .addToBackStack(null)
                            .commit()
                    }
                    recyclerView.adapter = adapter
                }
            }

            override fun onFailure(call: Call<List<Attraction>>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }
}