<template>
  <div class="drug-manage">
    <div class="toolbar">
      <el-input v-model="searchName" placeholder="搜索药品名称" clearable style="width: 240px" @clear="loadDrugs" @keyup.enter="loadDrugs" />
      <el-select v-model="filterActive" placeholder="上下架状态" @change="loadDrugs" style="width: 140px; margin-left: 12px">
        <el-option label="全部" :value="-1" />
        <el-option label="已上架" :value="1" />
        <el-option label="已下架" :value="0" />
      </el-select>
      <el-button type="primary" @click="loadDrugs" style="margin-left: 12px">搜索</el-button>
      <el-button type="success" @click="showAddDialog" style="margin-left: 8px">添加药品</el-button>
      <el-button @click="loadDrugs">刷新</el-button>
    </div>

    <el-table :data="drugs" v-loading="loading" stripe style="width: 100%; margin-top: 16px">
      <el-table-column prop="drugId" label="ID" width="70" />
      <el-table-column prop="drugName" label="药品名称" min-width="140" />
      <el-table-column prop="specification" label="规格" width="120" />
      <el-table-column prop="manufacturer" label="厂家" min-width="140" />
      <el-table-column prop="unitPrice" label="单价(元)" width="100">
        <template #default="{ row }">{{ row.unitPrice?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column prop="stockQuantity" label="库存" width="80" />
      <el-table-column prop="unit" label="单位" width="60" />
      <el-table-column label="库存状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.stockStatus === '库存充足' ? 'success' : row.stockStatus === '库存紧张' ? 'warning' : 'danger'" size="small">
            {{ row.stockStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isActive === 1 ? 'success' : 'info'" size="small">
            {{ row.isActive === 1 ? '已上架' : '已下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="showEditDialog(row)">编辑</el-button>
          <el-button type="success" size="small" @click="showStockInDialog(row)">入库</el-button>
          <el-button type="warning" size="small" @click="showStockOutDialog(row)">出库</el-button>
          <el-button v-if="row.isActive === 1" type="danger" size="small" @click="handleToggleActive(row)">下架</el-button>
          <el-button v-else type="success" size="small" @click="handleToggleActive(row)">上架</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑药品' : '添加药品'" width="560px">
      <el-form :model="drugForm" label-width="100px">
        <el-form-item label="药品名称" required>
          <el-input v-model="drugForm.drugName" placeholder="请输入药品名称" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="drugForm.specification" placeholder="如：0.25g*12片" />
        </el-form-item>
        <el-form-item label="生产厂家">
          <el-input v-model="drugForm.manufacturer" placeholder="请输入生产厂家" />
        </el-form-item>
        <el-form-item label="单位" required>
          <el-input v-model="drugForm.unit" placeholder="盒/瓶/支" />
        </el-form-item>
        <el-form-item label="单价(元)" required>
          <el-input-number v-model="drugForm.unitPrice" :min="0" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="初始库存">
          <el-input-number v-model="drugForm.stockQuantity" :min="0" :step="1" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="drugForm.description" type="textarea" :rows="3" placeholder="药品描述/说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- Stock In/Out Dialog -->
    <el-dialog v-model="stockDialogVisible" :title="stockDialogTitle" width="400px">
      <el-form :model="stockForm" label-width="100px">
        <el-form-item label="药品">
          <span>{{ stockTarget?.drugName }}</span>
        </el-form-item>
        <el-form-item label="当前库存">
          <span>{{ stockTarget?.stockQuantity }} {{ stockTarget?.unit }}</span>
        </el-form-item>
        <el-form-item label="数量" required>
          <el-input-number v-model="stockForm.quantity" :min="1" :step="1" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="stockForm.remark" placeholder="操作备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStock" :loading="stocking">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { listDrugs, createDrug, updateDrug, stockIn, stockOut, type DrugVO, type DrugDTO } from "@/api/admin/drug";

const loading = ref(false);
const saving = ref(false);
const stocking = ref(false);
const searchName = ref("");
const filterActive = ref<number>(-1);
const drugs = ref<DrugVO[]>([]);
const dialogVisible = ref(false);
const isEdit = ref(false);
const editingId = ref<number | null>(null);

const drugForm = reactive<DrugDTO>({
  drugName: "",
  specification: "",
  manufacturer: "",
  unit: "盒",
  unitPrice: 0,
  stockQuantity: 0,
  description: "",
});

const stockDialogVisible = ref(false);
const stockIsIn = ref(true);
const stockTarget = ref<DrugVO | null>(null);
const stockForm = reactive({ quantity: 1, remark: "" });

const stockDialogTitle = ref("");

const loadDrugs = async () => {
  loading.value = true;
  try {
    const params: Record<string, unknown> = {};
    if (searchName.value) params.name = searchName.value;
    if (filterActive.value !== -1) params.isActive = filterActive.value;
    drugs.value = await listDrugs(params);
  } finally {
    loading.value = false;
  }
};

const showAddDialog = () => {
  isEdit.value = false;
  editingId.value = null;
  drugForm.drugName = "";
  drugForm.specification = "";
  drugForm.manufacturer = "";
  drugForm.unit = "盒";
  drugForm.unitPrice = 0;
  drugForm.stockQuantity = 0;
  drugForm.description = "";
  dialogVisible.value = true;
};

const showEditDialog = (drug: DrugVO) => {
  isEdit.value = true;
  editingId.value = drug.drugId;
  drugForm.drugId = drug.drugId;
  drugForm.drugName = drug.drugName;
  drugForm.specification = drug.specification || "";
  drugForm.manufacturer = drug.manufacturer || "";
  drugForm.unit = drug.unit;
  drugForm.unitPrice = drug.unitPrice;
  drugForm.stockQuantity = drug.stockQuantity;
  drugForm.description = drug.description || "";
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (!drugForm.drugName) {
    ElMessage.warning("请输入药品名称");
    return;
  }
  if (!drugForm.unit) {
    ElMessage.warning("请输入单位");
    return;
  }
  saving.value = true;
  try {
    if (isEdit.value && editingId.value) {
      await updateDrug({ ...drugForm, drugId: editingId.value });
      ElMessage.success("更新成功");
    } else {
      await createDrug(drugForm);
      ElMessage.success("添加成功");
    }
    dialogVisible.value = false;
    await loadDrugs();
  } finally {
    saving.value = false;
  }
};

const handleToggleActive = async (drug: DrugVO) => {
  const newActive = drug.isActive === 1 ? 0 : 1;
  const action = newActive === 0 ? '下架' : '上架';
  try {
    await ElMessageBox.confirm(`确定${action}药品「${drug.drugName}」吗？`, `确认${action}`, { type: "warning" });
    await updateDrug({ drugId: drug.drugId, drugName: drug.drugName, isActive: newActive });
    ElMessage.success(`已${action}`);
    await loadDrugs();
  } catch { /* cancelled */ }
};

const showStockInDialog = (drug: DrugVO) => {
  stockIsIn.value = true;
  stockTarget.value = drug;
  stockDialogTitle.value = "药品入库";
  stockForm.quantity = 1;
  stockForm.remark = "";
  stockDialogVisible.value = true;
};

const showStockOutDialog = (drug: DrugVO) => {
  stockIsIn.value = false;
  stockTarget.value = drug;
  stockDialogTitle.value = "药品出库";
  stockForm.quantity = 1;
  stockForm.remark = "";
  stockDialogVisible.value = true;
};

const handleStock = async () => {
  if (!stockTarget.value) return;
  stocking.value = true;
  try {
    if (stockIsIn.value) {
      await stockIn(stockTarget.value.drugId, { quantity: stockForm.quantity, remark: stockForm.remark });
      ElMessage.success("入库成功");
    } else {
      await stockOut(stockTarget.value.drugId, { quantity: stockForm.quantity, remark: stockForm.remark });
      ElMessage.success("出库成功");
    }
    stockDialogVisible.value = false;
    await loadDrugs();
  } finally {
    stocking.value = false;
  }
};

onMounted(() => loadDrugs());
</script>

<style lang="scss" scoped>
.drug-manage {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}
.toolbar {
  display: flex;
  align-items: center;
}
</style>
