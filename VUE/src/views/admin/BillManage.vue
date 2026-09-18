<template>
  <div class="bill-manage">
    <h2>收费管理</h2>
    <div class="toolbar">
      <el-select v-model="filterStatus" placeholder="支付状态" clearable @change="loadBills" style="width: 160px">
        <el-option label="全部" :value="undefined" />
        <el-option label="未支付" :value="0" />
        <el-option label="已支付" :value="2" />
      </el-select>
      <el-button @click="loadBills" style="margin-left: 12px">刷新</el-button>
    </div>
    <el-table :data="bills" v-loading="loading" stripe style="margin-top: 16px">
      <el-table-column prop="billId" label="账单号" width="80" />
      <el-table-column prop="patientName" label="患者" width="100" />
      <el-table-column label="总金额" width="100">
        <template #default="{ row }">¥{{ row.totalAmount?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 2 ? 'success' : 'danger'" size="small">{{ row.statusDesc }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="paymentMethod" label="支付方式" width="100" />
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ row.createTime?.substring(0, 19).replace('T', ' ') }}</template>
      </el-table-column>
      <el-table-column label="支付时间" width="170">
        <template #default="{ row }">{{ row.payTime ? row.payTime.substring(0, 19).replace('T', ' ') : '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="showDetail(row.billId)">详情</el-button>
          <el-button v-if="row.status !== 2" type="success" size="small" @click="handleMarkPaid(row)">标记已付</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="账单详情" width="560px">
      <el-descriptions :column="2" border v-if="billDetail">
        <el-descriptions-item label="账单号">{{ billDetail.billId }}</el-descriptions-item>
        <el-descriptions-item label="患者">{{ billDetail.patientName }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ billDetail.totalAmount?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ billDetail.statusDesc }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ billDetail.paymentMethod || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="billDetail?.items || []" stripe style="margin-top: 16px">
        <el-table-column prop="itemTypeDesc" label="类型" width="80" />
        <el-table-column prop="itemName" label="项目" min-width="140" />
        <el-table-column label="单价" width="80">
          <template #default="{ row }">¥{{ row.unitPrice?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="60" />
        <el-table-column label="小计" width="80">
          <template #default="{ row }">¥{{ row.amount?.toFixed(2) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { ElMessage } from "element-plus";
import { getAllBills, markAsPaid, getBillDetail } from "@/api/admin/billing";
import type { BillVO } from "@/api/patient/billing";

const bills = ref<BillVO[]>([]);
const loading = ref(false);
const filterStatus = ref<number | undefined>(undefined);
const dialogVisible = ref(false);
const billDetail = ref<BillVO | null>(null);

const loadBills = async () => {
  loading.value = true;
  try { bills.value = await getAllBills(filterStatus.value); }
  finally { loading.value = false; }
};

const showDetail = async (billId: number) => {
  try {
    billDetail.value = await getBillDetail(billId);
    dialogVisible.value = true;
  } catch {
    ElMessage.error("获取账单详情失败");
  }
};

const handleMarkPaid = async (bill: BillVO) => {
  try {
    await markAsPaid(bill.billId, "现金");
    ElMessage.success("已标记为已付");
    await loadBills();
  } catch { /* ignore */ }
};

loadBills();
</script>

<style lang="scss" scoped>
.bill-manage { padding: 20px; h2 { margin: 0 0 16px 0; } }
.toolbar { display: flex; align-items: center; }
</style>
