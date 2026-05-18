<template>
  <section class="card-view">
    <component v-for="(card, index) in paginatedData" :key="getCardKey(card, index)" :is="props.myCard"
      v-bind="typeof card === 'object' ? card : {}" @click="handleclick(card)" />
    <el-pagination class="card-pagination" layout="prev, pager, next" background size="large"
      v-model:current-page="currentPage" :total="props.cardsprops.length" :page-size="pageSize"
      v-show="props.cardsprops.length >= pageSize" />
  </section>
</template>

<script lang="ts" setup>
import { type Component, type PropType, computed, ref } from 'vue';

// 分页组件相关
const currentPage = ref(1);
const pageSize = ref(12); // 每页显示的数量
const paginatedData = computed<CardPayload[]>(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return props.cardsprops.slice(start, end);
});

const props = defineProps({
  myCard: {
    type: Object as PropType<Component>,
    required: true
  },
  cardsprops: {
    type: Array as PropType<CardPayload[]>,
    required: true
  },
});

type CardPayload = Record<string, unknown> | string | number | boolean | null | undefined;

const getCardKey = (card: CardPayload, index: number): string => {
  if (card && typeof card === 'object') {
    const obj = card as Record<string, unknown>;
    const candidate =
      obj.scheduleId ??
      obj.doctorId ??
      obj.clinicId ??
      obj.deptId ??
      obj.departmentId ??
      obj.id ??
      obj.userId ??
      obj.appointmentId;

    if (typeof candidate === 'string' || typeof candidate === 'number') {
      return String(candidate);
    }
  }
  return String(index);
};

const emit = defineEmits(['backpage', 'nextpage', 'handleclick']);
const handleclick = (item: CardPayload) => {
  emit('handleclick', item);
}

</script>

<style lang="scss" scoped>
.card-view {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  /* 可控间距：支持分别控制上下(row)与左右(column) */
  column-gap: var(--card-gap-x, var(--card-gap, var(--space-4)));
  row-gap: var(--card-gap-y, var(--card-gap, var(--space-4)));
  align-items: stretch;
}

.card-pagination {
  width: 100%;
  display: flex;
  justify-content: center;
  margin-top: var(--space-4);
}
</style>
