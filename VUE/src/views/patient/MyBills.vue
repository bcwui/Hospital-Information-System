<template>
  <div class="my-bills">
    <h2>我的账单</h2>
    <el-table :data="bills" v-loading="loading" stripe>
      <el-table-column prop="billId" label="账单号" width="100" />
      <el-table-column label="时间" width="180">
        <template #default="{ row }">{{ row.createTime?.substring(0, 19).replace('T', ' ') }}</template>
      </el-table-column>
      <el-table-column label="总金额" width="120">
        <template #default="{ row }">¥{{ row.totalAmount?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 2 ? 'success' : 'danger'" size="small">{{ row.statusDesc }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="paymentMethod" label="支付方式" width="100" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="showDetail(row.billId)">详情</el-button>
          <el-button v-if="row.status !== 2" type="success" size="small" @click="showPayDialog(row)">支付</el-button>
          <el-button v-if="row.status === 2" size="small" @click="showInvoice(row.billId)">发票</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && bills.length === 0" description="暂无账单" />

    <!-- Detail dialog -->
    <el-dialog v-model="dialogVisible" title="账单详情" width="560px">
      <el-descriptions :column="2" border v-if="billDetail">
        <el-descriptions-item label="账单号">{{ billDetail.billId }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ billDetail.statusDesc }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ billDetail.totalAmount?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ billDetail.paymentMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ billDetail.createTime?.substring(0, 19).replace('T', ' ') }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ billDetail.payTime ? billDetail.payTime.substring(0, 19).replace('T', ' ') : '-' }}</el-descriptions-item>
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

    <!-- Payment dialog -->
    <el-dialog v-model="payDialogVisible" title="模拟支付" width="360px">
      <div style="text-align: center; margin-bottom: 16px">
        <span style="font-size: 24px; font-weight: 700; color: #f56c6c">¥{{ payTarget?.totalAmount?.toFixed(2) }}</span>
      </div>
      <el-radio-group v-model="payMethod" style="display: flex; flex-direction: column; gap: 8px">
        <el-radio value="微信">微信支付</el-radio>
        <el-radio value="支付宝">支付宝</el-radio>
        <el-radio value="现金">现金</el-radio>
        <el-radio value="医保卡">医保卡</el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="payDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePay" :loading="paying">确认支付</el-button>
      </template>
    </el-dialog>

    <!-- Invoice dialog -->
    <el-dialog v-model="invoiceDialogVisible" title="发票" width="480px">
      <div v-if="invoice" class="invoice" id="invoice-print">
        <h1 style="text-align: center">仁康医院门诊收费发票</h1>
        <div class="invoice-info">
          <p>发票号：{{ invoice.billId }}</p>
          <p>患者：{{ invoice.patientName }}</p>
          <p>金额：¥{{ invoice.totalAmount?.toFixed(2) }}</p>
          <p>支付方式：{{ invoice.paymentMethod }}</p>
          <p>状态：{{ invoice.statusDesc }}</p>
          <p>时间：{{ invoice.createTime?.substring(0, 19).replace('T', ' ') }}</p>
        </div>
        <table class="invoice-table">
          <thead><tr><th>项目</th><th>单价</th><th>数量</th><th>金额</th></tr></thead>
          <tbody>
            <tr v-for="item in invoice.items" :key="item.itemId">
              <td>{{ item.itemName }}</td>
              <td>¥{{ item.unitPrice?.toFixed(2) }}</td>
              <td>{{ item.quantity }}</td>
              <td>¥{{ item.amount?.toFixed(2) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <template #footer>
        <el-button type="primary" @click="printInvoice">打印</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { ElMessage } from "element-plus";
import { getMyBills, getBillDetail, payBill, getInvoice, type BillVO, type InvoiceVO } from "@/api/patient/billing";

const bills = ref<BillVO[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const billDetail = ref<BillVO | null>(null);
const payDialogVisible = ref(false);
const payTarget = ref<BillVO | null>(null);
const payMethod = ref("微信");
const paying = ref(false);
const invoiceDialogVisible = ref(false);
const invoice = ref<InvoiceVO | null>(null);

const loadBills = async () => {
  loading.value = true;
  try { bills.value = await getMyBills(); }
  finally { loading.value = false; }
};

const showDetail = async (billId: number) => {
  billDetail.value = await getBillDetail(billId);
  dialogVisible.value = true;
};

const showPayDialog = (bill: BillVO) => {
  payTarget.value = bill;
  payMethod.value = "微信";
  payDialogVisible.value = true;
};

const handlePay = async () => {
  if (!payTarget.value) return;
  paying.value = true;
  try {
    await payBill(payTarget.value.billId, payMethod.value);
    ElMessage.success("支付成功");
    payDialogVisible.value = false;
    await loadBills();
  } finally { paying.value = false; }
};

const showInvoice = async (billId: number) => {
  invoice.value = await getInvoice(billId);
  invoiceDialogVisible.value = true;
};

const printInvoice = () => {
  window.print();
};

loadBills();
</script>

<style lang="scss" scoped>
.my-bills { padding: 20px; h2 { margin: 0 0 16px 0; } }
.invoice {
  h1 { font-size: 18px; margin-bottom: 16px; }
  .invoice-info { margin-bottom: 12px; p { margin: 4px 0; font-size: 14px; } }
  .invoice-table {
    width: 100%; border-collapse: collapse;
    th, td { border: 1px solid #e2e8f0; padding: 8px; text-align: left; font-size: 13px; }
    th { background: #f8fafc; }
  }
}
@media print {
  body * { visibility: hidden; }
  #invoice-print, #invoice-print * { visibility: visible; }
  #invoice-print { position: absolute; left: 0; top: 0; }
}
</style>
