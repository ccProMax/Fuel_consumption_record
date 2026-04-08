package com.example.fuelrecord

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat

/**
 * 油耗记录列表适配器
 */
class FuelRecordAdapter(
    private var recordsWithDistance: List<FuelRecordWithDistance>,
    private val onDeleteClick: (FuelRecord) -> Unit,
    private val onEditClick: (FuelRecord) -> Unit
) : RecyclerView.Adapter<FuelRecordAdapter.RecordViewHolder>() {

    private val decimalFormat = DecimalFormat("#.##")
    private val moneyFormat = DecimalFormat("#.00")

    class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvConsumption: TextView = itemView.findViewById(R.id.tvConsumption)
        val tvMileage: TextView = itemView.findViewById(R.id.tvMileage)
        val tvDistanceAdded: TextView = itemView.findViewById(R.id.tvDistanceAdded)
        val tvFuelAmount: TextView = itemView.findViewById(R.id.tvFuelAmount)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvTotalCost: TextView = itemView.findViewById(R.id.tvTotalCost)
        val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        val tvFuelType: TextView = itemView.findViewById(R.id.tvFuelType)
        val tvFullFlag: TextView = itemView.findViewById(R.id.tvFullFlag)
        val btnMore: ImageButton = itemView.findViewById(R.id.btnMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fuel_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val item = recordsWithDistance[position]
        val record = item.record

        // 左上角：里程
        holder.tvMileage.text = "${decimalFormat.format(record.mileage)} km"

        // 增加里程（如果有前一条记录）
        if (item.hasPreviousRecord && item.distanceAdded > 0) {
            holder.tvDistanceAdded.text = "+${decimalFormat.format(item.distanceAdded)} km"
            holder.tvDistanceAdded.visibility = View.VISIBLE
        } else {
            holder.tvDistanceAdded.visibility = View.GONE
        }

        // 右上角：油耗
        if (record.fuelConsumption > 0) {
            holder.tvConsumption.text = "${decimalFormat.format(record.fuelConsumption)} L/100km"
        } else {
            holder.tvConsumption.text = if (record.isFull) "无法计算" else "未加满"
        }

        // 左下角：时间
        holder.tvDate.text = record.getFormattedDate()

        holder.tvFuelAmount.text = "${decimalFormat.format(record.fuelAmount)} L"
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

        // 三点菜单按钮
        holder.btnMore.setOnClickListener {
            showPopupMenu(it, record)
        }
    }

    private fun showPopupMenu(view: View, record: FuelRecord) {
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.record_actions_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    onEditClick(record)
                    true
                }
                R.id.action_delete -> {
                    onDeleteClick(record)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    override fun getItemCount(): Int = recordsWithDistance.size

    fun updateRecords(newRecordsWithDistance: List<FuelRecordWithDistance>) {
        recordsWithDistance = newRecordsWithDistance
        notifyDataSetChanged()
    }
}
