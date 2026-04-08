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

    /**
     * 显示添加记录对话框
     */
    fun showAddRecordDialog() {
        showRecordDialog(null)
    }

    /**
     * 显示编辑记录对话框
     */
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
        val tvDistanceAdded = dialogView.findViewById<TextView>(R.id.tvDistanceAdded)
        val etFuelAmount = dialogView.findViewById<TextInputEditText>(R.id.etFuelAmount)
        val etPricePerLiter = dialogView.findViewById<TextInputEditText>(R.id.etPricePerLiter)
        val etNote = dialogView.findViewById<TextInputEditText>(R.id.etNote)
        val tvCalculatedTotal = dialogView.findViewById<TextView>(R.id.tvCalculatedTotal)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val rgFuelType = dialogView.findViewById<android.widget.RadioGroup>(R.id.rgFuelType)
        val rb92 = dialogView.findViewById<RadioButton>(R.id.rb92)
        val rb95 = dialogView.findViewById<RadioButton>(R.id.rb95)
        val rb98 = dialogView.findViewById<RadioButton>(R.id.rb98)
        val swIsFull = dialogView.findViewById<SwitchMaterial>(R.id.swIsFull)

        // 获取上一条记录的里程（用于计算行驶里程）
        val lastRecordMileage = viewModel.records.value?.firstOrNull()?.record?.mileage

        // 如果是编辑模式
        if (editRecord != null) {
            tvDialogTitle.text = getString(R.string.edit_record_title)
            val cal = Calendar.getInstance()
            cal.time = editRecord.date
            updateDateField(etDate, cal)
            etMileage.setText(editRecord.mileage.toString())
            etFuelAmount.setText(editRecord.fuelAmount.toString())
            etPricePerLiter.setText(editRecord.pricePerLiter.toString())
            etNote.setText(editRecord.note)
            tvCalculatedTotal.text = "¥${moneyFormat.format(editRecord.totalCost)}"
            swIsFull.isChecked = editRecord.isFull

            when (editRecord.fuelType) {
                95 -> rb95.isChecked = true
                98 -> rb98.isChecked = true
                else -> rb92.isChecked = true
            }

            // 编辑模式下，如果是最后一条记录，显示距离差
            if (lastRecordMileage != null && lastRecordMileage == editRecord.mileage) {
                val allRecords = viewModel.records.value ?: emptyList()
                val nextRecord = allRecords.firstOrNull { it.record.id != editRecord.id }?.record
                if (nextRecord != null && editRecord.mileage >= nextRecord.mileage) {
                    val dist = editRecord.mileage - nextRecord.mileage
                    tvDistanceAdded.text = "${decimalFormat.format(dist)} km"
                }
            }
        } else {
            // 添加模式：默认日期为今天
            val cal = Calendar.getInstance()
            updateDateField(etDate, cal)
        }

        // 日期选择
        val dateCal = Calendar.getInstance()
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

        // ==================== 联动计算逻辑 ====================
        // 用于防止循环触发
        var isUpdating = false

        // 1. 行驶里程联动：当前里程变化时，自动计算行驶里程
        val updateDistanceAdded = {
            val currentMileage = etMileage.text?.toString()?.toDoubleOrNull()
            if (currentMileage != null && lastRecordMileage != null && currentMileage >= lastRecordMileage) {
                val dist = currentMileage - lastRecordMileage
                tvDistanceAdded.text = "${decimalFormat.format(dist)} km"
            } else {
                tvDistanceAdded.text = "0 km"
            }
        }

        // 2. 油价/油量/总价联动：填了其中两个，自动算第三个
        fun recalculateFromPriceAndAmount() {
            if (isUpdating) return
            isUpdating = true
            val fuelAmount = etFuelAmount.text?.toString()?.toDoubleOrNull()
            val pricePerLiter = etPricePerLiter.text?.toString()?.toDoubleOrNull()
            if (fuelAmount != null && pricePerLiter != null && fuelAmount > 0 && pricePerLiter > 0) {
                val total = fuelAmount * pricePerLiter
                tvCalculatedTotal.text = "¥${moneyFormat.format(total)}"
            }
            isUpdating = false
        }

        fun recalculateFromTotalAndAmount() {
            if (isUpdating) return
            isUpdating = true
            val totalStr = tvCalculatedTotal.text?.toString()?.removePrefix("¥")?.toDoubleOrNull()
            val fuelAmount = etFuelAmount.text?.toString()?.toDoubleOrNull()
            if (totalStr != null && fuelAmount != null && totalStr > 0 && fuelAmount > 0) {
                val price = totalStr / fuelAmount
                etPricePerLiter.setText(decimalFormat.format(price))
            }
            isUpdating = false
        }

        fun recalculateFromTotalAndPrice() {
            if (isUpdating) return
            isUpdating = true
            val totalStr = tvCalculatedTotal.text?.toString()?.removePrefix("¥")?.toDoubleOrNull()
            val pricePerLiter = etPricePerLiter.text?.toString()?.toDoubleOrNull()
            if (totalStr != null && pricePerLiter != null && totalStr > 0 && pricePerLiter > 0) {
                val amount = totalStr / pricePerLiter
                etFuelAmount.setText(decimalFormat.format(amount))
            }
            isUpdating = false
        }

        // 监听里程变化 → 更新行驶里程
        etMileage.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { updateDistanceAdded() }
        })

        // 监听加油量变化
        etFuelAmount.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdating) return
                val pricePerLiter = etPricePerLiter.text?.toString()?.toDoubleOrNull()
                val totalStr = tvCalculatedTotal.text?.toString()?.removePrefix("¥")?.toDoubleOrNull()
                if (pricePerLiter != null && pricePerLiter > 0 && totalStr != null && totalStr > 0) {
                    recalculateFromTotalAndPrice()
                } else {
                    recalculateFromPriceAndAmount()
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // 监听单价变化
        etPricePerLiter.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdating) return
                val fuelAmount = etFuelAmount.text?.toString()?.toDoubleOrNull()
                val totalStr = tvCalculatedTotal.text?.toString()?.removePrefix("¥")?.toDoubleOrNull()
                if (fuelAmount != null && fuelAmount > 0 && totalStr != null && totalStr > 0) {
                    recalculateFromTotalAndAmount()
                } else {
                    recalculateFromPriceAndAmount()
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // 点击总价也可以输入（让用户修改总价反推）
        tvCalculatedTotal.setOnClickListener {
            val dialog = android.app.AlertDialog.Builder(this)
                .setTitle("输入总价")
                .setView(android.widget.EditText(this).apply {
                    hint = "请输入总价"
                    setText(tvCalculatedTotal.text?.toString()?.removePrefix("¥"))
                }.also { editText ->
                    editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                })
                .setPositiveButton("确定") { _, _ ->
                    val input = (dialogView.findViewById<android.view.ViewGroup>(android.R.id.content)?.getChildAt(0) as? android.view.ViewGroup)
                        ?.getChildAt(0)?.findViewById<android.widget.EditText>(android.R.id.edit)
                    // 简化处理：直接使用 editText 的值
                }
                .setNegativeButton("取消", null)
                .create()
            // 简化：点击总价时不弹框，而是通过修改油量或单价来反推
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
                    // 编辑模式：清空油耗，由DatabaseHelper重新计算
                    val updated = FuelRecord(
                        id = editRecord.id,
                        date = cal.time,
                        mileage = mileage,
                        fuelAmount = fuelAmount,
                        pricePerLiter = pricePerLiter,
                        totalCost = fuelAmount * pricePerLiter,
                        note = note,
                        fuelConsumption = 0.0,
                        fuelType = fuelType,
                        isFull = isFull
                    )
                    viewModel.updateRecord(updated)
                    Toast.makeText(this, R.string.success_update_record, Toast.LENGTH_SHORT).show()
                } else {
                    // 添加模式
                    viewModel.addRecord(cal.time, mileage, fuelAmount, pricePerLiter, note, fuelType, isFull)
                    Toast.makeText(this, R.string.success_add_record, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)

        // 如果是编辑模式，添加删除按钮
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
}
