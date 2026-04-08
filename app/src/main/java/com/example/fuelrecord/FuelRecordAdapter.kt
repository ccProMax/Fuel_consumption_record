package com.example.fuelrecord

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat

/**
 * 油耗记录列表适配器
 */
class FuelRecordAdapter(
    private var records: List<FuelRecord>,
    private val onDeleteClick: (FuelRecord) -> Unit,
    private val onEditClick: (FuelRecord) -> Unit
) : RecyclerView.Adapter<FuelRecordAdapter.RecordViewHolder>() {

    private val decimalFormat = DecimalFormat("#.##")
    private val moneyFormat = DecimalFormat("#.00")

    class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvConsumption: TextView = itemView.findViewById(R.id.tvConsumption)
        val tvMileage: TextView = itemView.findViewById(R.id.tvMileage)
        val tvFuelAmount: TextView = itemView.findViewById(R.id.tvFuelAmount)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvTotalCost: TextView = itemView.findViewById(R.id.tvTotalCost)
        val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        val tvFuelType: TextView = itemView.findViewById(R.id.tvFuelType)
        val tvFullFlag: TextView = itemView.findViewById(R.id.tvFullFlag)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fuel_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val record = records[position]

        holder.tvDate.text = record.getFormattedDate()

        // 油耗显示
        if (record.fuelConsumption > 0) {
            holder.tvConsumption.text = "${decimalFormat.format(record.fuelConsumption)} L/100km"
        } else {
            holder.tvConsumption.text = if (record.isFull) "等待下次加满" else "未加满"
        }

        holder.tvMileage.text = "里程: ${decimalFormat.format(record.mileage)} km"
        holder.tvFuelAmount.text = "加油: ${decimalFormat.format(record.fuelAmount)} L"
        holder.tvPrice.text = "¥${moneyFormat.format(record.pricePerLiter)}/L"
        holder.tvTotalCost.text = "¥${moneyFormat.format(record.totalCost)}"
        holder.tvFuelType.text = record.getFuelTypeLabel()

        // 是否加满标识
        holder.tvFullFlag.text = if (record.isFull) "加满" else "未加满"
        holder.tvFullFlag.setTextColor(
            if (record.isFull)
                holder.itemView.context.getColor(R.color.success_color)
            else
                holder.itemView.context.getColor(R.color.warning_color)
        )

        // 备注
        if (record.note.isNotBlank()) {
            holder.tvNote.text = record.note
            holder.tvNote.visibility = View.VISIBLE
        } else {
            holder.tvNote.visibility = View.GONE
        }

        holder.btnDelete.setOnClickListener { onDeleteClick(record) }
        holder.btnEdit.setOnClickListener { onEditClick(record) }
    }

    override fun getItemCount(): Int = records.size

    fun updateRecords(newRecords: List<FuelRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }
}
