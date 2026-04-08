package com.example.fuelrecord

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fuelrecord.databinding.FragmentRecordsBinding

class RecordsFragment : Fragment() {

    private var _binding: FragmentRecordsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FuelViewModel by lazy {
        (requireActivity() as MainActivity).viewModel
    }
    private lateinit var adapter: FuelRecordAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeData()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = FuelRecordAdapter(emptyList(),
            onDeleteClick = { record -> showDeleteConfirmDialog(record) },
            onEditClick = { record -> (requireActivity() as MainActivity).showEditRecordDialog(record) }
        )
        binding.rvRecords.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@RecordsFragment.adapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    private fun observeData() {
        viewModel.records.observe(viewLifecycleOwner) { records ->
            adapter.updateRecords(records)
            if (records.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvRecords.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvRecords.visibility = View.VISIBLE
            }
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAddRecord.setOnClickListener {
            (requireActivity() as MainActivity).showAddRecordDialog()
        }
        binding.btnExport.setOnClickListener {
            exportToCsv()
        }
    }

    private fun showDeleteConfirmDialog(record: FuelRecord) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(R.string.confirm_delete_message)
            .setPositiveButton(R.string.confirm) { _, _ ->
                viewModel.deleteRecord(record.id)
                Toast.makeText(requireContext(), R.string.success_delete_record, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun exportToCsv() {
        val records = viewModel.records.value
        if (records.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "没有可导出的记录", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val csvContent = buildCsvContent(records)
            val fileName = "fuel_records_${System.currentTimeMillis()}.csv"
            val file = java.io.File(requireContext().filesDir, fileName)
            file.writeText(csvContent)
            Toast.makeText(requireContext(), "文件已保存到: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildCsvContent(records: List<FuelRecord>): String {
        val sb = StringBuilder()
        sb.append("日期,里程(km),加油量(L),单价(元/L),总价(元),油耗(L/100km),汽油标号,是否加满,备注\n")
        records.reversed().forEach { record ->
            sb.append("${record.getFormattedDate()},")
            sb.append("${record.mileage},")
            sb.append("${record.fuelAmount},")
            sb.append("${record.pricePerLiter},")
            sb.append("${record.totalCost},")
            sb.append("${record.fuelConsumption},")
            sb.append("${record.fuelType},")
            sb.append("${if (record.isFull) "是" else "否"},")
            sb.append("\"${record.note}\"\n")
        }
        return sb.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
