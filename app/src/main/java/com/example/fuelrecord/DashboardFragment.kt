package com.example.fuelrecord

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.fuelrecord.databinding.FragmentDashboardBinding
import java.text.DecimalFormat

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FuelViewModel by lazy {
        (requireActivity() as MainActivity).viewModel
    }

    private val decimalFormat = DecimalFormat("#.##")
    private val moneyFormat = DecimalFormat("#.00")

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
        val currentMonths = viewModel.dashboardMonths
        when (currentMonths) {
            3 -> binding.rb3Month.isChecked = true
            12 -> binding.rb12Month.isChecked = true
            else -> binding.rb6Month.isChecked = true
        }

        binding.rgPeriod.setOnCheckedChangeListener { _, checkedId ->
            val months = when (checkedId) {
                R.id.rb3Month -> 3
                R.id.rb12Month -> 12
                else -> 6
            }
            viewModel.dashboardMonths = months
        }
    }

    private fun observeData() {
        viewModel.dashboardStats.observe(viewLifecycleOwner) { stats ->
            binding.tvAvgConsumption.text = decimalFormat.format(stats.avgConsumption)
            binding.tvPricePer100km.text = decimalFormat.format(stats.avgPricePer100km)
            binding.tvTotalDistance.text = decimalFormat.format(stats.totalDistance)
            binding.tvTotalFuel.text = decimalFormat.format(stats.totalFuelAmount)
            binding.tvTotalCost.text = moneyFormat.format(stats.totalCost)
            binding.tvAvgPrice.text = decimalFormat.format(stats.avgPricePerLiter)
        }

        viewModel.consumptionChartData.observe(viewLifecycleOwner) { points ->
            binding.chartConsumption.setData(points, Color.parseColor("#FF9800"))
        }

        viewModel.costPer100kmChartData.observe(viewLifecycleOwner) { points ->
            binding.chartCostPer100km.setData(points, Color.parseColor("#2196F3"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
