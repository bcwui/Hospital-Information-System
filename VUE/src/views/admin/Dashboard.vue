<template>
  <div class="dashboard">
    <h2>数据看板</h2>

    <!-- Today summary cards -->
    <div class="summary-cards">
      <div class="summary-card" v-for="card in summaryCards" :key="card.label" :style="{ borderTopColor: card.color }">
        <div class="card-label">{{ card.label }}</div>
        <div class="card-value" :style="{ color: card.color }">{{ card.value }}</div>
      </div>
    </div>

    <!-- Trend charts -->
    <div class="chart-row">
      <div class="chart-box">
        <h3>挂号趋势</h3>
        <div class="chart-controls">
          <el-radio-group v-model="apptDays" size="small" @change="loadApptTrend">
            <el-radio-button :value="7">近7天</el-radio-button>
            <el-radio-button :value="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
        <div ref="apptChartRef" class="chart"></div>
      </div>
      <div class="chart-box">
        <h3>收入趋势</h3>
        <div class="chart-controls">
          <el-radio-group v-model="revenueDays" size="small" @change="loadRevenueTrend">
            <el-radio-button :value="7">近7天</el-radio-button>
            <el-radio-button :value="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
        <div ref="revenueChartRef" class="chart"></div>
      </div>
    </div>

    <!-- Distribution + Workload -->
    <div class="chart-row">
      <div class="chart-box">
        <h3>科室就诊分布</h3>
        <div ref="deptChartRef" class="chart"></div>
      </div>
      <div class="chart-box">
        <h3>医生工作量</h3>
        <div ref="doctorChartRef" class="chart"></div>
      </div>
    </div>

    <!-- AI stats -->
    <div class="chart-row">
      <div class="chart-box full-width">
        <h3>AI 问诊统计</h3>
        <div class="ai-stats">
          <div class="ai-stat-item">
            <span class="ai-stat-label">总会话数</span>
            <span class="ai-stat-value">{{ aiStats?.totalSessions || 0 }}</span>
          </div>
          <div class="ai-stat-item">
            <span class="ai-stat-label">今日会话</span>
            <span class="ai-stat-value">{{ aiStats?.todaySessions || 0 }}</span>
          </div>
          <div class="ai-stat-item">
            <span class="ai-stat-label">平均消息数/会话</span>
            <span class="ai-stat-value">{{ aiStats?.avgMessagesPerSession || 0 }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from "vue";
import * as echarts from "echarts";
import {
  getTodayStats, getAppointmentTrend, getRevenueTrend,
  getDeptDistribution, getDoctorWorkload, getAIStats,
  type DashboardTodayVO, type DashboardTrendVO, type DashboardDeptVO,
  type DashboardDoctorWorkloadVO, type DashboardAIVO
} from "@/api/admin/dashboard";

const summaryCards = ref([
  { label: "今日挂号", value: "0", color: "#0ea5e9" },
  { label: "今日收入", value: "¥0", color: "#10b981" },
  { label: "新增患者", value: "0", color: "#f59e0b" },
  { label: "AI咨询", value: "0", color: "#6366f1" },
]);

const aiStats = ref<DashboardAIVO | null>(null);

const apptDays = ref(7);
const revenueDays = ref(7);
const apptChartRef = ref<HTMLElement | null>(null);
const revenueChartRef = ref<HTMLElement | null>(null);
const deptChartRef = ref<HTMLElement | null>(null);
const doctorChartRef = ref<HTMLElement | null>(null);

let apptChart: echarts.ECharts | null = null;
let revenueChart: echarts.ECharts | null = null;
let deptChart: echarts.ECharts | null = null;
let doctorChart: echarts.ECharts | null = null;

onMounted(async () => {
  await loadTodayStats();
  await loadApptTrend();
  await loadRevenueTrend();
  await loadDeptDist();
  await loadDoctorWorkload();
  await loadAIStats();
});

const loadTodayStats = async () => {
  const stats = await getTodayStats();
  summaryCards.value = [
    { label: "今日挂号", value: String(stats.appointmentCount), color: "#0ea5e9" },
    { label: "今日收入", value: "¥" + (stats.revenue || 0).toFixed(2), color: "#10b981" },
    { label: "新增患者", value: String(stats.newPatients), color: "#f59e0b" },
    { label: "AI咨询", value: String(stats.aiConsultCount), color: "#6366f1" },
  ];
};

const loadApptTrend = async () => {
  const data = await getAppointmentTrend(apptDays.value);
  await nextTick();
  if (!apptChartRef.value) return;
  if (!apptChart) apptChart = echarts.init(apptChartRef.value);
  apptChart.setOption({
    tooltip: { trigger: "axis" },
    xAxis: { type: "category", data: data.map(d => d.date.substring(5)) },
    yAxis: { type: "value" },
    series: [{ data: data.map(d => d.count), type: "line", smooth: true, areaStyle: { color: "rgba(14,165,233,0.15)" }, lineStyle: { color: "#0ea5e9" }, itemStyle: { color: "#0ea5e9" } }],
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
  });
};

const loadRevenueTrend = async () => {
  const data = await getRevenueTrend(revenueDays.value);
  await nextTick();
  if (!revenueChartRef.value) return;
  if (!revenueChart) revenueChart = echarts.init(revenueChartRef.value);
  revenueChart.setOption({
    tooltip: { trigger: "axis", valueFormatter: (v: number) => "¥" + (v || 0).toFixed(2) },
    xAxis: { type: "category", data: data.map(d => d.date.substring(5)) },
    yAxis: { type: "value" },
    series: [{ data: data.map(d => d.amount || 0), type: "line", smooth: true, areaStyle: { color: "rgba(16,185,129,0.15)" }, lineStyle: { color: "#10b981" }, itemStyle: { color: "#10b981" } }],
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
  });
};

const loadDeptDist = async () => {
  const data = await getDeptDistribution();
  await nextTick();
  if (!deptChartRef.value) return;
  if (!deptChart) deptChart = echarts.init(deptChartRef.value);
  deptChart.setOption({
    tooltip: { trigger: "item" },
    series: [{
      type: "pie", radius: ["45%", "75%"], center: ["50%", "55%"],
      data: data.map(d => ({ name: d.deptName, value: d.count })),
      label: { formatter: "{b}\n{d}%" },
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: "rgba(0,0,0,0.2)" } },
    }],
  });
};

const loadDoctorWorkload = async () => {
  const data = await getDoctorWorkload();
  await nextTick();
  if (!doctorChartRef.value) return;
  if (!doctorChart) doctorChart = echarts.init(doctorChartRef.value);
  doctorChart.setOption({
    tooltip: { trigger: "axis", axisPointer: { type: "shadow" } },
    xAxis: { type: "category", data: data.map(d => d.doctorName), axisLabel: { rotate: 30 } },
    yAxis: { type: "value" },
    series: [{ data: data.map(d => d.count), type: "bar", itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: "#6366f1" }, { offset: 1, color: "#a5b4fc" }]) }, barMaxWidth: 36 }],
    grid: { left: 40, right: 20, top: 20, bottom: 60 },
  });
};

const loadAIStats = async () => {
  aiStats.value = await getAIStats();
};
</script>

<style lang="scss" scoped>
.dashboard { padding: 20px; overflow-y: auto; h2 { margin: 0 0 16px 0; } }
.summary-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.summary-card {
  background: #fff; border-radius: 12px; padding: 20px; border-top: 4px solid;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  .card-label { font-size: 13px; color: #94a3b8; margin-bottom: 8px; }
  .card-value { font-size: 28px; font-weight: 700; }
}
.chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
.chart-box {
  background: #fff; border-radius: 12px; padding: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  h3 { margin: 0 0 8px 0; font-size: 15px; }
  .chart-controls { margin-bottom: 8px; }
  .chart { width: 100%; height: 300px; }
  &.full-width { grid-column: 1 / -1; }
}
.ai-stats { display: flex; gap: 40px; padding: 20px 0; justify-content: center; }
.ai-stat-item { text-align: center; .ai-stat-label { display: block; font-size: 13px; color: #94a3b8; margin-bottom: 4px; } .ai-stat-value { font-size: 24px; font-weight: 700; color: #6366f1; } }
</style>
