<template>
  <article class="department-card surface-card" :class="cardThemeClass" @click="emit('click')">
    <header class="department-header">
      <div class="department-title-block">
        <p class="eyebrow">{{ isClinicCard ? '门诊' : '科室' }}</p>
        <h3 class="department-title">{{ props.name }}</h3>
      </div>
      <span class="status-pill" :class="pillClass">{{ statusText }}</span>
    </header>

    <div class="department-content">
      <p class="helper-text" v-if="isClinicCard">
        点击查看该门诊医生列表
      </p>
      <p class="helper-text" v-else>
        点击查看该科室门诊列表
      </p>
    </div>
    <div id="btns" v-if="props.cardType === 'admin'">
      <el-button type="success" @click.stop="() => { timeTableVisible = true; }">自动排班</el-button>
      <el-button type="primary" @click.stop="updateName">修改</el-button>
      <el-button type="danger" @click.stop="deleteItem">删除</el-button>
    </div>
  </article>
  <el-dialog v-model="timeTableVisible" title="请填写选择排班时间信息" width="800">
    <TimeForm optionType="autoUpdate" @autoUpdate="autoUpdate"></TimeForm>
  </el-dialog>
</template>

<script lang="ts" setup>
import TimeForm from './TimeForm.vue';
import { ref, computed } from 'vue';
import { useHospitalStore } from '@/stores/hospitalData';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRoute } from 'vue-router';
import { autoUpdateSchedules } from '@/api/admin/registrations';

const route = useRoute();
const hospitalStore = useHospitalStore();
const timeTableVisible = ref(false);
const emit = defineEmits(['click']);
const props = defineProps({
  cardType: {
    type: String,
    required: true
  },
  name: {
    type: String,
    required: true
  },
  state: {
    type: Number,
    required: true
  },
  id: {
    type: String,
    required: true
  }
})

const isClinicCard = computed(() => {
  // 在“门诊列表”页（已有 departmentId）时，此卡片代表门诊；否则代表科室
  const deptId = route.query.departmentId
  return Array.isArray(deptId) ? Boolean(deptId[0]) : Boolean(deptId)
})

const cardThemeClass = computed(() => (isClinicCard.value ? 'theme-clinic' : 'theme-department'))

const statusText = computed(() => {
  if (props.state === 0) return '暂时关闭'
  if (props.state === 1) return '正常开放'
  return '发生故障'
})

const pillClass = computed(() => {
  // base.css 里只有 is-success / is-warning
  return props.state === 1 ? 'is-success' : 'is-warning'
})

const resolveDepartmentId = (): string | undefined => {
  const deptId = route.query.departmentId;
  if (Array.isArray(deptId)) {
    const firstValue = deptId[0];
    return typeof firstValue === 'string' && firstValue.length > 0 ? firstValue : undefined;
  }
  return typeof deptId === 'string' && deptId.length > 0 ? deptId : undefined;
};
const autoUpdate = async (startDate: string, endDate: string) => {
  const departmentId = resolveDepartmentId();
  let res;
  if (departmentId) {
    // 在门诊列表页，为该门诊排班
    res = await autoUpdateSchedules(startDate, endDate, props.id);
  } else {
    // 在科室列表页，为该科室下所有门诊排班
    res = await autoUpdateSchedules(startDate, endDate, undefined, props.id);
  }
  if (res !== undefined) {
    const targetLabel = departmentId ? '门诊' : '科室';
    ElMessage({
      message: `${targetLabel}"${props.name}"在${startDate}至${endDate}的排班已自动创建`,
      type: 'success',
    });
    timeTableVisible.value = false;
  }
}
const updateName = async () => {
  let newName = '';
  try {
    const res = await ElMessageBox.prompt('请输入新名称', '修改名称', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: props.name,
      inputPlaceholder: '请输入名称',
    });
    newName = res.value.trim();
  } catch {
    return;
  }
  if (!newName) return;

  const departmentId = resolveDepartmentId();
  if (!departmentId) {
    await hospitalStore.updateDepart(props.id, newName);
  } else {
    await hospitalStore.updateClinic(props.id, newName, departmentId);
  }
  ElMessage({
    message: '修改成功',
    type: 'success',
  });
}

const deleteItem = async () => {
  const departmentId = resolveDepartmentId();
  try {
    const targetLabel = departmentId ? '门诊' : '科室';
    await ElMessageBox.confirm(`确定要删除${targetLabel}“${props.name}”吗？此操作不可恢复。`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }
  if (!departmentId) {
    await hospitalStore.deleteDepart(props.id);
  } else {
    await hospitalStore.deleteClinic(props.id, departmentId);
  }
  ElMessage({
    message: '删除成功',
    type: 'success',
  })
}
</script>

<style lang="scss" scoped>
.department-card {
  cursor: pointer;
  width: 300px;
  height: 200px;
  padding: var(--space-4);
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  border: 1px solid transparent;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: linear-gradient(90deg, #0ea5e9, #6366f1, #8b5cf6);
    opacity: 0;
    transition: opacity 0.35s ease;
    border-radius: 0;
  }

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 16px 32px rgba(14, 165, 233, 0.12);
    border-color: rgba(14, 165, 233, 0.15);

    &::before {
      opacity: 1;
    }

    .department-title {
      background: linear-gradient(135deg, #0ea5e9, #6366f1);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
  }
}

.department-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
}

.department-title-block {
  min-width: 0;
}

.eyebrow {
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-muted);
  font-weight: 600;
}

.department-title {
  font-size: 18px;
  font-weight: 700;
  margin-top: 4px;
  color: var(--color-text);
  line-height: 1.2;
  word-break: break-word;
  transition: all 0.3s ease;
}

.department-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.helper-text {
  color: var(--color-text-muted);
  text-align: center;
  font-size: 0.9rem;
  padding: 0 8px;
}

#btns {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 8px;

  :deep(.el-button) {
    border-radius: 8px;
    font-size: 0.82rem;
    font-weight: 500;
    transition: all 0.2s ease;

    &:hover {
      transform: translateY(-1px);
    }
  }
}
</style>
