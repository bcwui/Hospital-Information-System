<template>
  <div class="prescription-view">
    <!-- Patient selector -->
    <div class="header">
      <div class="patient-select">
        <span class="label">患者：</span>
        <el-select
          v-model="patientId"
          filterable
          remote
          reserve-keyword
          placeholder="输入患者姓名或身份证号搜索"
          :remote-method="searchPatientsRemote"
          :loading="searchingPatient"
          clearable
          @change="onPatientSelected"
          style="width: 340px"
        >
          <el-option
            v-for="p in patientOptions"
            :key="p.patientId"
            :label="`${p.name} — ${p.idCard}`"
            :value="p.patientId"
          />
        </el-select>
        <span v-if="patientName" class="patient-name">当前患者：{{ patientName }}</span>
      </div>
    </div>

    <div class="main-content" v-if="patientId">
      <div class="left-panel">
        <!-- Drug search -->
        <div class="drug-search">
          <el-input v-model="drugSearchName" placeholder="搜索药品名称" clearable @keyup.enter="searchDrugsList" @clear="searchDrugsList">
            <template #append>
              <el-button @click="searchDrugsList" :loading="searchingDrugs">搜索</el-button>
            </template>
          </el-input>
        </div>

        <!-- Drug search results -->
        <div class="drug-list" v-loading="searchingDrugs">
          <el-table :data="drugResults" stripe max-height="400" highlight-current-row @row-click="addToPrescription">
            <el-table-column prop="drugName" label="药品名称" min-width="120" />
            <el-table-column prop="specification" label="规格" width="100" />
            <el-table-column label="单价" width="80">
              <template #default="{ row }">¥{{ row.unitPrice?.toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="库存" width="80">
              <template #default="{ row }">
                <el-tag :type="row.stockQuantity > 10 ? 'success' : row.stockQuantity > 0 ? 'warning' : 'danger'" size="small">
                  {{ row.stockQuantity }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="60">
              <template #default>
                <el-button type="primary" size="small" circle>+</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div class="right-panel">
        <!-- Prescription items table -->
        <div class="prescription-items">
          <h3>处方明细（共 {{ prescriptionItems.length }} 种药品）</h3>
          <el-table :data="prescriptionItems" stripe>
            <el-table-column label="序号" type="index" width="60" />
            <el-table-column prop="drugName" label="药品名称" min-width="120" />
            <el-table-column prop="specification" label="规格" width="100" />
            <el-table-column prop="dosage" label="用量" width="100" />
            <el-table-column prop="usageMethod" label="用法" width="80" />
            <el-table-column prop="frequency" label="频率" width="100" />
            <el-table-column prop="days" label="天数" width="60" />
            <el-table-column label="数量" width="60">
              <template #default="{ row }">{{ row.quantity }}{{ row.unit }}</template>
            </el-table-column>
            <el-table-column label="小计" width="80">
              <template #default="{ row }">¥{{ (row.unitPrice * row.quantity).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ $index }">
                <el-button type="warning" size="small" @click="editItem($index)">编辑</el-button>
                <el-button type="danger" size="small" @click="removeItem($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="prescription-footer" v-if="prescriptionItems.length > 0">
            <span class="total-amount">总金额：¥{{ totalAmount.toFixed(2) }}</span>
            <el-button type="success" size="large" @click="submitPrescription" :loading="submitting">提交处方</el-button>
          </div>
        </div>

        <!-- History -->
        <div class="prescription-history">
          <h3>处方历史</h3>
          <el-table :data="history" stripe v-loading="loadingHistory" max-height="300">
            <el-table-column prop="prescriptionId" label="处方号" width="80" />
            <el-table-column prop="createTime" label="时间" width="170">
              <template #default="{ row }">{{ row.createTime?.substring(0, 19).replace('T', ' ') }}</template>
            </el-table-column>
            <el-table-column label="药品数" width="70">
              <template #default="{ row }">{{ row.details?.length || 0 }}</template>
            </el-table-column>
            <el-table-column label="总金额" width="100">
              <template #default="{ row }">¥{{ row.totalAmount?.toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 2 ? 'success' : row.status === 1 ? 'primary' : 'info'" size="small">
                  {{ row.statusDesc }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button type="primary" size="small" link @click="viewHistoryDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- Edit prescription item dialog -->
    <el-dialog v-model="editDialogVisible" title="编辑用药详情" width="480px">
      <el-form :model="editForm" label-width="100px" v-if="editForm">
        <el-form-item label="药品">{{ editForm.drugName }}</el-form-item>
        <el-form-item label="单次用量" required>
          <el-input v-model="editForm.dosage" placeholder="如：0.25g / 1片" />
        </el-form-item>
        <el-form-item label="用法" required>
          <el-select v-model="editForm.usageMethod" placeholder="选择用法">
            <el-option label="口服" value="口服" />
            <el-option label="外用" value="外用" />
            <el-option label="静脉注射" value="静脉注射" />
            <el-option label="皮下注射" value="皮下注射" />
            <el-option label="舌下含服" value="舌下含服" />
            <el-option label="直肠给药" value="直肠给药" />
          </el-select>
        </el-form-item>
        <el-form-item label="频率" required>
          <el-select v-model="editForm.frequency" placeholder="选择频率">
            <el-option label="每日1次" value="每日1次" />
            <el-option label="每日2次" value="每日2次" />
            <el-option label="每日3次" value="每日3次" />
            <el-option label="每日4次" value="每日4次" />
            <el-option label="睡前1次" value="睡前1次" />
            <el-option label="必要时" value="必要时" />
          </el-select>
        </el-form-item>
        <el-form-item label="天数">
          <el-input-number v-model="editForm.days" :min="1" :max="90" :step="1" />
        </el-form-item>
        <el-form-item label="数量" required>
          <el-input-number v-model="editForm.quantity" :min="1" :max="999" :step="1" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmEdit">确认</el-button>
      </template>
    </el-dialog>

    <!-- History detail dialog -->
    <el-dialog v-model="historyDialogVisible" title="处方详情" width="600px">
      <el-descriptions :column="2" border v-if="historyDetail">
        <el-descriptions-item label="处方号">{{ historyDetail.prescriptionId }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ historyDetail.statusDesc }}</el-descriptions-item>
        <el-descriptions-item label="患者">{{ historyDetail.patientName }}</el-descriptions-item>
        <el-descriptions-item label="医生">{{ historyDetail.doctorName }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ historyDetail.createTime?.substring(0, 19).replace('T', ' ') }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ historyDetail.totalAmount?.toFixed(2) }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="historyDetail?.details || []" stripe style="margin-top: 16px">
        <el-table-column prop="drugName" label="药品" />
        <el-table-column prop="dosage" label="用量" />
        <el-table-column prop="usageMethod" label="用法" />
        <el-table-column prop="frequency" label="频率" />
        <el-table-column prop="days" label="天数" />
        <el-table-column label="小计">
          <template #default="{ row }">¥{{ row.subtotal?.toFixed(2) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue";
import { ElMessage } from "element-plus";
import {
  searchDrugs, submitPrescription as submitPrescriptionApi,
  getPatientPrescriptions, getPrescriptionDetail,
  searchPatients,
  type DrugVO, type PrescriptionVO, type PrescriptionDetailDTO, type PatientVO
} from "@/api/doctor/prescription";

const patientId = ref<number>();   // el-select 的 model 不接受 null，用 undefined 表示"未选择患者"
const patientName = ref("");
const searchingPatient = ref(false);
const patientOptions = ref<PatientVO[]>([]);

const drugSearchName = ref("");
const drugResults = ref<DrugVO[]>([]);
const searchingDrugs = ref(false);

interface RxItem extends DrugVO {
  dosage: string;
  usageMethod: string;
  frequency: string;
  days: number;
  remark: string;
  quantity: number;   // 处方明细数量：代码里一直在用（编辑弹窗、提交、回显），原类型漏了 → vue-tsc 报 4 处错
}

const prescriptionItems = ref<RxItem[]>([]);
const submitting = ref(false);
const history = ref<PrescriptionVO[]>([]);
const loadingHistory = ref(false);

const editDialogVisible = ref(false);
const editingIndex = ref(-1);
const editForm = ref<RxItem | null>(null);

const historyDialogVisible = ref(false);
const historyDetail = ref<PrescriptionVO | null>(null);

const totalAmount = computed(() => {
  return prescriptionItems.value.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0);
});

const searchPatientsRemote = async (query: string) => {
  if (!query || query.trim().length < 1) {
    patientOptions.value = [];
    return;
  }
  searchingPatient.value = true;
  try {
    patientOptions.value = await searchPatients(query.trim());
  } finally {
    searchingPatient.value = false;
  }
};

const onPatientSelected = async (selectedId: number | null) => {
  if (!selectedId) {
    patientName.value = "";
    history.value = [];
    return;
  }
  const selected = patientOptions.value.find(p => p.patientId === selectedId);
  patientName.value = selected ? selected.name : `患者#${selectedId}`;
  await loadHistory();
};

const searchDrugsList = async () => {
  searchingDrugs.value = true;
  try {
    const name = drugSearchName.value?.trim();
    drugResults.value = await searchDrugs(name || undefined);
  } finally {
    searchingDrugs.value = false;
  }
};

onMounted(() => {
  searchDrugsList();
});

const addToPrescription = (drug: DrugVO) => {
  const existing = prescriptionItems.value.findIndex(item => item.drugId === drug.drugId);
  const newItem: RxItem = {
    ...drug,
    dosage: "",
    usageMethod: "口服",
    frequency: "每日3次",
    days: 3,
    quantity: 1,
    remark: "",
  };
  if (existing >= 0) {
    editingIndex.value = existing;
    editForm.value = { ...prescriptionItems.value[existing] };
    editDialogVisible.value = true;
  } else {
    prescriptionItems.value.push(newItem);
    editingIndex.value = prescriptionItems.value.length - 1;
    editForm.value = { ...newItem };
    editDialogVisible.value = true;
  }
};

const editItem = (index: number) => {
  editingIndex.value = index;
  editForm.value = { ...prescriptionItems.value[index] };
  editDialogVisible.value = true;
};

const removeItem = (index: number) => {
  prescriptionItems.value.splice(index, 1);
};

const confirmEdit = () => {
  if (!editForm.value) return;
  if (!editForm.value.dosage) { ElMessage.warning("请输入用量"); return; }
  if (!editForm.value.usageMethod) { ElMessage.warning("请选择用法"); return; }
  if (!editForm.value.frequency) { ElMessage.warning("请选择频率"); return; }
  prescriptionItems.value[editingIndex.value] = { ...editForm.value };
  editDialogVisible.value = false;
};

const submitPrescription = async () => {
  if (!patientId.value) { ElMessage.warning("请先选择患者"); return; }
  if (prescriptionItems.value.length === 0) { ElMessage.warning("请添加处方药品"); return; }

  const items: PrescriptionDetailDTO[] = prescriptionItems.value.map(item => ({
    drugId: item.drugId,
    quantity: item.quantity,
    dosage: item.dosage,
    usageMethod: item.usageMethod,
    frequency: item.frequency,
    days: item.days,
    remark: item.remark,
  }));

  submitting.value = true;
  try {
    await submitPrescriptionApi({ patientId: patientId.value, items });
    ElMessage.success("处方提交成功");
    prescriptionItems.value = [];
    await loadHistory();
  } finally {
    submitting.value = false;
  }
};

const loadHistory = async () => {
  if (!patientId.value) return;
  loadingHistory.value = true;
  try {
    history.value = await getPatientPrescriptions(patientId.value);
  } finally {
    loadingHistory.value = false;
  }
};

const viewHistoryDetail = async (prescription: PrescriptionVO) => {
  try {
    historyDetail.value = await getPrescriptionDetail(prescription.prescriptionId);
    historyDialogVisible.value = true;
  } catch {
    // ignore
  }
};
</script>

<style lang="scss" scoped>
.prescription-view {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
}
.header {
  .patient-select {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .label { font-weight: 600; white-space: nowrap; }
  .patient-name { color: #409eff; font-weight: 600; }
}
.main-content {
  display: flex;
  gap: 20px;
  flex: 1;
  min-height: 0;
}
.left-panel {
  width: 380px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}
.drug-list {
  flex: 1;
  overflow-y: auto;
}
.prescription-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  .total-amount { font-size: 18px; font-weight: 700; color: #f56c6c; }
}
h3 { margin: 0 0 8px 0; font-size: 15px; color: #303133; }
</style>
