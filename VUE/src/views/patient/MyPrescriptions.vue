<template>
  <div class="my-prescriptions">
    <h2>我的处方</h2>
    <el-table :data="prescriptions" v-loading="loading" stripe>
      <el-table-column prop="prescriptionId" label="处方号" width="100" />
      <el-table-column prop="doctorName" label="医生" width="120" />
      <el-table-column label="时间" width="180">
        <template #default="{ row }">{{ row.createTime?.substring(0, 19).replace('T', ' ') }}</template>
      </el-table-column>
      <el-table-column label="药品数" width="80">
        <template #default="{ row }">{{ row.details?.length || 0 }}</template>
      </el-table-column>
      <el-table-column label="总金额" width="120">
        <template #default="{ row }">¥{{ row.totalAmount?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 2 ? 'success' : row.status === 1 ? 'primary' : 'info'" size="small">
            {{ row.statusDesc }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="showDetail(row.prescriptionId)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && prescriptions.length === 0" description="暂无处方记录" />

    <el-dialog v-model="dialogVisible" title="处方详情" width="600px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="处方号">{{ detail.prescriptionId }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.statusDesc }}</el-descriptions-item>
        <el-descriptions-item label="医生">{{ detail.doctorName }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ detail.createTime?.substring(0, 19).replace('T', ' ') }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ detail.totalAmount?.toFixed(2) }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail?.details || []" stripe style="margin-top: 16px">
        <el-table-column prop="drugName" label="药品" min-width="120" />
        <el-table-column prop="specification" label="规格" width="100" />
        <el-table-column prop="dosage" label="用量" width="100" />
        <el-table-column prop="usageMethod" label="用法" width="80" />
        <el-table-column prop="frequency" label="频率" width="100" />
        <el-table-column prop="days" label="天数" width="60" />
        <el-table-column label="小计" width="80">
          <template #default="{ row }">¥{{ row.subtotal?.toFixed(2) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { getMyPrescriptions, getMyPrescriptionDetail, type PrescriptionVO } from "@/api/doctor/prescription";

const prescriptions = ref<PrescriptionVO[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const detail = ref<PrescriptionVO | null>(null);

const loadPrescriptions = async () => {
  loading.value = true;
  try {
    prescriptions.value = await getMyPrescriptions();
  } finally {
    loading.value = false;
  }
};

const showDetail = async (prescriptionId: number) => {
  try {
    detail.value = await getMyPrescriptionDetail(prescriptionId);
    dialogVisible.value = true;
  } catch { /* ignore */ }
};

loadPrescriptions();
</script>

<style lang="scss" scoped>
.my-prescriptions {
  padding: 20px;
  h2 { margin: 0 0 16px 0; }
}
</style>
