package com.simple.ui.precompute

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.t.databinding.ItemCardBinding
import com.simple.t.databinding.ItemFooterBinding
import com.simple.t.databinding.ItemSectionBinding

data class SectionViewItem(val label: String) : ViewItem {
    override fun areItemsTheSame(): List<Any> = listOf(label)
}

class SectionAdapter : ViewItemAdapter<SectionViewItem, ItemSectionBinding>() {
    override val viewItemClass: Class<SectionViewItem> = SectionViewItem::class.java

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemSectionBinding {
        return ItemSectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(binding: ItemSectionBinding, viewType: Int, position: Int, item: SectionViewItem) {
        binding.label.text = item.label
    }
}

data class CardViewItem(val id: String, val result: LayoutResult) : ViewItem {
    override fun areItemsTheSame(): List<Any> = listOf(id)
}

class CardAdapter : ViewItemAdapter<CardViewItem, ItemCardBinding>() {
    override val viewItemClass: Class<CardViewItem> = CardViewItem::class.java

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemCardBinding {
        return ItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(binding: ItemCardBinding, viewType: Int, position: Int, item: CardViewItem) {
        binding.precomputedView.result = item.result
    }
}

object FooterViewItem : ViewItem {
    override fun areItemsTheSame(): List<Any> = listOf("footer")
}

class FooterAdapter : ViewItemAdapter<FooterViewItem, ItemFooterBinding>() {
    override val viewItemClass: Class<FooterViewItem> = FooterViewItem::class.java

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemFooterBinding {
        return ItemFooterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }
}
