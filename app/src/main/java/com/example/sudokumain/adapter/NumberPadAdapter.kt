package com.example.sudokumain.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sudokumain.R

/**
 * Adapter for the number pad in the game screen.
 * Displays numbers 1-9 for input into the Sudoku board.
 */
class NumberPadAdapter(
    private val numbers: List<Int>,
    private val onNumberSelected: (Int) -> Unit
) : RecyclerView.Adapter<NumberPadAdapter.NumberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_number_pad, parent, false)
        return NumberViewHolder(view)
    }

    override fun onBindViewHolder(holder: NumberViewHolder, position: Int) {
        val number = numbers[position]
        holder.bind(number)
    }

    override fun getItemCount(): Int = numbers.size

    inner class NumberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNumber: TextView = itemView.findViewById(R.id.tvNumber)

        fun bind(number: Int) {
            tvNumber.text = number.toString()
            
            itemView.setOnClickListener {
                onNumberSelected(number)
            }
        }
    }
}
