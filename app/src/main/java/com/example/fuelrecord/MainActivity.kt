package com.example.fuelrecord

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.fuelrecord.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import java.text.DecimalFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    val viewModel: FuelViewModel by viewModels()

    private val decimalFormat = DecimalFormat("#.##")
    private val moneyFormat = DecimalFormat("#.00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
    }

    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_records)
                1 -> getString(R.string.tab_dashboard)
                else -> ""
            }
        }.attach()
    }

    inner class ViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 2
        override fun createFragment(position: Int) = when (position) {
            0 -> RecordsFragment()
            1 -> DashboardFragment()
            else -> RecordsFragment()
        }
    }

    fun showAddRecordDialog() {
        showRecordDialog(null)
    }

    fun showEditRecordDialog(record: FuelRecord) {
        showRecordDialog(record)
    }

    /**
     * 显示添加/编辑记录对话框
     */
    private fun showRecordDialog(editRecord: FuelRecord?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_record, null)
        val etDate = dialogView.findViewById<TextInputEditText>(R.id.etDate)
        val etMileage = dialogView.findViewById<TextInputEditText>(R.id.etMileage)
        val etDistanceAdded = dialogView.findViewById<TextInputEditText>(R.id.etDistanceAdded)
        val etFuelAmount = dialogView.findViewById<TextInputEditText>(R.id.etFuelAmount)
        val etPricePerLiter = dialogView.findViewById<TextInputEditText>(R.id.etPricePerLiter)
        val etTotalCost = dialogView.findViewById<TextInputEditText>(R.id.etTotalCost)
        val etNote = dialogView.findViewById<TextInputEditText>(R.id.etNote)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val rgFuelType = dialogView.findViewById<android.widget.RadioGroup>(R.id.rgFuelType)
        val rb92 = dialogView.findViewById<RadioButton>(R.id.rb92)
        val rb95 = dialogView.findViewById<RadioButton>(R.id.rb95)
        val rb98 = dialogView.findViewById<RadioButton>(R.id.rb98)
        val swIsFull = dialogView.findViewById<SwitchMaterial>(R.id.swIsFull)

        val recordsWithDistance = viewModel.records.value ?: emptyList()

        // 获取前一条记录的里程（用于计算行驶里程）
        // 添加模式：取最新一条记录的里程
        // 编辑模式：取当前记录在列表中"下一个"记录（即日期更早的那条）的里程
        val prevRecordMileage: Double? = if (editRecord != null) {
            val currentIndex = recordsWithDistance.indexOfFirst { it.record.id == editRecord.id }
            if (currentIndex >= 0 && currentIndex < recordsWithDistance.size - 1) {
                recordsWithDistance[currentIndex + 1].record.mileage
            } else {
                null
            }
        } else {
            recordsWithDistance.firstOrNull()?.record?.mileage
        }

        // 如果是编辑模式
        if (editRecord != null) {
            tvDialogTitle.text = getString(R.string.edit_record_title)
            val cal = Calendar.getInstance()
            cal.time = editRecord.date
            updateDateField(etDate, cal)
            etMileage.setText(editRecord.mileage.toString())
            etFuelAmount.setText(editRecord.fuelAmount.toString())
            etPricePerLiter.setText(editRecord.pricePerLiter.toString())
            etTotalCost.setText(moneyFormat.format(editRecord.totalCost))
            etNote.setText(editRecord.note)
            swIsFull.isChecked = editRecord.isFull

            when (editRecord.fuelType) {
                95 -> rb95.isChecked = true
                98 -> rb98.isChecked = true
                else -> rb92.isChecked = true
            }

            // 编辑模式下，计算并显示距离差
            if (prevRecordMileage != null && editRecord.mileage >= prevRecordMileage) {
                val dist = editRecord.mileage - prevRecordMileage
                etDistanceAdded.setText(decimalFormat.format(dist))
            }
        } else {
            // 添加模式：默认日期为今天
            val cal = Calendar.getInstance()
            updateDateField(etDate, cal)
        }

        // ==================== 联动计算逻辑 ====================
        // 核心思路：用 beforeTextChanged 记住旧值，在 afterTextChanged 中判断是用户输入还是程序设置
        // 只有用户真正修改了某个字段，才更新关联字段，避免 setText 导致的循环触发

        // 里程联动：当前里程 ↔ 行驶里程
        // 油价/油量/总价联动：三者中填任意两个，自动算第三个

        // 里程联动
        setupMileageLinking(etMileage, etDistanceAdded, prevRecordMileage)

        // 价格/油量/总价联动
        setupPriceLinking(etFuelAmount, etPricePerLiter, etTotalCost)

        // 日期选择
        etDate.setOnClickListener {
            val cal = Calendar.getInstance()
            val dateStr = etDate.text?.toString()
            if (!dateStr.isNullOrBlank() && dateStr.length >= 10) {
                val parts = dateStr.split("-")
                if (parts.size == 3) {
                    cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                }
            }
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    cal.set(year, month, dayOfMonth)
                    updateDateField(etDate, cal)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // 解析日期
        fun parseDate(): Calendar? {
            val dateStr = etDate.text?.toString() ?: return null
            if (dateStr.length < 10) return null
            val parts = dateStr.split("-")
            if (parts.size != 3) return null
            return Calendar.getInstance().apply {
                set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            }
        }

        val builder = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton(if (editRecord == null) R.string.btn_save else R.string.btn_save) { _, _ ->
                val cal = parseDate()
                val mileage = etMileage.text?.toString()?.toDoubleOrNull()
                val fuelAmount = etFuelAmount.text?.toString()?.toDoubleOrNull()
                val pricePerLiter = etPricePerLiter.text?.toString()?.toDoubleOrNull()
                val totalCost = etTotalCost.text?.toString()?.toDoubleOrNull()
                    ?: fuelAmount?.times(pricePerLiter ?: 0.0) ?: 0.0
                val note = etNote.text?.toString() ?: ""
                val fuelType = when {
                    rb95.isChecked -> 95
                    rb98.isChecked -> 98
                    else -> 92
                }
                val isFull = swIsFull.isChecked

                if (cal == null || mileage == null || mileage <= 0) {
                    Toast.makeText(this, R.string.error_invalid_mileage, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (fuelAmount == null || fuelAmount <= 0) {
                    Toast.makeText(this, R.string.error_invalid_fuel_amount, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (pricePerLiter == null || pricePerLiter <= 0) {
                    Toast.makeText(this, R.string.error_invalid_price, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (editRecord != null) {
                    val updated = FuelRecord(
                        id = editRecord.id,
                        date = cal.time,
                        mileage = mileage,
                        fuelAmount = fuelAmount,
                        pricePerLiter = pricePerLiter,
                        totalCost = totalCost,
                        note = note,
                        fuelConsumption = 0.0,
                        fuelType = fuelType,
                        isFull = isFull
                    )
                    viewModel.updateRecord(updated)
                    Toast.makeText(this, R.string.success_update_record, Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addRecord(cal.time, mileage, fuelAmount, pricePerLiter, note, fuelType, isFull)
                    Toast.makeText(this, R.string.success_add_record, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)

        if (editRecord != null) {
            builder.setNeutralButton(R.string.delete_record) { _, _ ->
                viewModel.deleteRecord(editRecord.id)
                Toast.makeText(this, R.string.success_delete_record, Toast.LENGTH_SHORT).show()
            }
        }

        builder.show()
    }

    private fun updateDateField(editText: android.widget.EditText, calendar: Calendar) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        editText.setText(String.format("%04d-%02d-%02d", year, month, day))
    }

    // ==================== 里程联动 ====================

    private fun setupMileageLinking(
        etMileage: TextInputEditText,
        etDistanceAdded: TextInputEditText,
        prevRecordMileage: Double?
    ) {
        if (prevRecordMileage == null) return

        lateinit var mileageWatcher: android.text.TextWatcher
        lateinit var distanceWatcher: android.text.TextWatcher

        mileageWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val current = s?.toString()?.toDoubleOrNull()
                if (current != null && current >= prevRecordMileage) {
                    val dist = current - prevRecordMileage
                    val newText = decimalFormat.format(dist)
                    val currentText = etDistanceAdded.text?.toString() ?: ""
                    if (currentText != newText) {
                        etDistanceAdded.removeTextChangedListener(distanceWatcher)
                        etDistanceAdded.setText(newText)
                        etDistanceAdded.setSelection(newText.length)
                        etDistanceAdded.addTextChangedListener(distanceWatcher)
                    }
                } else {
                    etDistanceAdded.removeTextChangedListener(distanceWatcher)
                    if (etDistanceAdded.text?.isNotEmpty() == true) {
                        etDistanceAdded.setText("")
                    }
                    etDistanceAdded.addTextChangedListener(distanceWatcher)
                }
            }
        }

        distanceWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val dist = s?.toString()?.toDoubleOrNull()
                if (dist != null && dist >= 0) {
                    val mileage = prevRecordMileage + dist
                    val newText = decimalFormat.format(mileage)
                    val currentText = etMileage.text?.toString() ?: ""
                    if (currentText != newText) {
                        etMileage.removeTextChangedListener(mileageWatcher)
                        etMileage.setText(newText)
                        etMileage.setSelection(newText.length)
                        etMileage.addTextChangedListener(mileageWatcher)
                    }
                }
            }
        }

        // 添加监听器
        etMileage.addTextChangedListener(mileageWatcher)
        etDistanceAdded.addTextChangedListener(distanceWatcher)
    }

    // ==================== 价格联动 ====================

    /**
     * 设置油价/油量/总价的联动
     * 规则：
     * - 修改加油量：用单价×油量计算总价
     * - 修改单价：用单价×油量计算总价
     * - 修改总价：用总价÷单价计算油量
     */
    private fun setupPriceLinking(
        etFuelAmount: TextInputEditText,
        etPricePerLiter: TextInputEditText,
        etTotalCost: TextInputEditText
    ) {
        var amountUpdating = false
        var priceUpdating = false
        var totalUpdating = false

        // 修改加油量 → 用单价×油量 = 总价
        etFuelAmount.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (amountUpdating) return
                amountUpdating = true
                val amount = s?.toString()?.toDoubleOrNull()
                val price = etPricePerLiter.text?.toString()?.toDoubleOrNull()
                if (amount != null && amount > 0 && price != null && price > 0) {
                    val currentText = etTotalCost.text?.toString() ?: ""
                    val newText = moneyFormat.format(amount * price)
                    if (currentText != newText) {
                        val cursorPos = etTotalCost.selectionStart
                        etTotalCost.setText(newText)
                        // 恢复光标位置到末尾
                        etTotalCost.setSelection(newText.length)
                    }
                }
                amountUpdating = false
            }
        })

        // 修改单价 → 用单价×油量 = 总价
        etPricePerLiter.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (priceUpdating) return
                priceUpdating = true
                val price = s?.toString()?.toDoubleOrNull()
                val amount = etFuelAmount.text?.toString()?.toDoubleOrNull()
                if (price != null && price > 0 && amount != null && amount > 0) {
                    val currentText = etTotalCost.text?.toString() ?: ""
                    val newText = moneyFormat.format(amount * price)
                    if (currentText != newText) {
                        val cursorPos = etTotalCost.selectionStart
                        etTotalCost.setText(newText)
                        etTotalCost.setSelection(newText.length)
                    }
                }
                priceUpdating = false
            }
        })

        // 修改总价 → 用总价÷单价 = 油量
        etTotalCost.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (totalUpdating) return
                totalUpdating = true
                val total = s?.toString()?.toDoubleOrNull()
                val price = etPricePerLiter.text?.toString()?.toDoubleOrNull()
                if (total != null && total > 0 && price != null && price > 0) {
                    val currentText = etFuelAmount.text?.toString() ?: ""
                    val newText = decimalFormat.format(total / price)
                    if (currentText != newText) {
                        etFuelAmount.setText(newText)
                        etFuelAmount.setSelection(newText.length)
                    }
                } else if (total == null || total == 0.0) {
                    // 用户删除了总价，清空加油量
                    val currentText = etFuelAmount.text?.toString() ?: ""
                    if (currentText.isNotEmpty()) {
                        etFuelAmount.setText("")
                    }
                }
                totalUpdating = false
            }
        })
    }
}
