package com.ccpro.fuelrecord

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.ccpro.fuelrecord.databinding.FragmentDashboardBinding
import java.text.DecimalFormat
import java.util.Calendar

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FuelViewModel by lazy {
        (requireActivity() as MainActivity).viewModel
    }

    private val decimalFormat = DecimalFormat("#.##")
    private val moneyFormat = DecimalFormat("#.00")

    // 统计周期选项
    private val periodOptions = listOf(
        PeriodOption("近3月", 3),
        PeriodOption("近半年", 6),
        PeriodOption("今年来", -1),  // -1表示今年
        PeriodOption("近1年", 12),
        PeriodOption("近2年", 24),
        PeriodOption("近3年", 36)
    )

    data class PeriodOption(val label: String, val months: Int)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPeriodSelector()
        observeData()
    }

    private fun setupPeriodSelector() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            periodOptions.map { it.label }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPeriod.adapter = adapter

        // 设置当前选中的值
        val currentMonths = viewModel.dashboardMonths
        val currentIndex = periodOptions.indexOfFirst { it.months == currentMonths }
        if (currentIndex >= 0) {
            binding.spinnerPeriod.setSelection(currentIndex)
        }

        binding.spinnerPeriod.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedOption = periodOptions[position]
                // 设置dashboardMonths会自动触发loadDashboardData()
                viewModel.dashboardMonths = selectedOption.months
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun observeData() {
        // 观察周期性数据（受时间周期影响）
        viewModel.dashboardPeriodStats.observe(viewLifecycleOwner) { stats ->
            binding.tvAvgConsumption.text = decimalFormat.format(stats.avgConsumption)
            binding.tvPricePer100km.text = decimalFormat.format(stats.avgPricePerKm)
        }

        // 观察全局数据（不受时间周期影响）
        viewModel.dashboardGlobalStats.observe(viewLifecycleOwner) { stats ->
            binding.tvTotalDistance.text = decimalFormat.format(stats.totalDistance)
            binding.tvTotalFuel.text = decimalFormat.format(stats.totalFuelAmount)
            binding.tvTotalCost.text = moneyFormat.format(stats.totalCost)
            binding.tvAvgPrice.text = decimalFormat.format(stats.avgPricePerLiter)
        }

        viewModel.consumptionChartData.observe(viewLifecycleOwner) { points ->
            binding.chartConsumption.setData(points, Color.parseColor("#FF9800"))
        }

        viewModel.fuelPriceChartData.observe(viewLifecycleOwner) { points ->
            binding.chartFuelPrice.setData(points, Color.parseColor("#2196F3"))
        }

        viewModel.costPer100kmChartData.observe(viewLifecycleOwner) { points ->
            binding.chartCostPer100km.setData(points, Color.parseColor("#4CAF50"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
