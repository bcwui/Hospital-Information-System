<template>
  <div class="notification-center">
    <div class="header">
      <h2>消息通知</h2>
      <el-button v-if="notifications.some(n => n.isRead === 0)" type="primary" size="small" @click="handleMarkAllRead">全部已读</el-button>
    </div>
    <div class="list" v-loading="loading">
      <div v-for="item in notifications" :key="item.notificationId" class="notification-item" :class="{ unread: item.isRead === 0 }" @click="handleClick(item)">
        <div class="item-header">
          <span class="item-title">
            <span v-if="item.isRead === 0" class="unread-dot"></span>
            {{ item.title }}
          </span>
          <el-tag v-if="item.type === 'appointment_reminder'" size="small" type="warning">预约提醒</el-tag>
          <el-tag v-else-if="item.type === 'status_change'" size="small" type="primary">状态变更</el-tag>
          <el-tag v-else size="small" type="info">系统消息</el-tag>
        </div>
        <p class="item-content">{{ item.content }}</p>
        <span class="item-time">{{ item.createTime?.substring(0, 19).replace('T', ' ') }}</span>
      </div>
      <el-empty v-if="!loading && notifications.length === 0" description="暂无通知" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { getMyNotifications, markAsRead, markAllAsRead, type NotificationVO } from "@/api/notification";

const notifications = ref<NotificationVO[]>([]);
const loading = ref(false);

const loadNotifications = async () => {
  loading.value = true;
  try { notifications.value = await getMyNotifications(); }
  finally { loading.value = false; }
};

const handleClick = async (item: NotificationVO) => {
  if (item.isRead === 0) {
    await markAsRead(item.notificationId);
    item.isRead = 1;
  }
};

const handleMarkAllRead = async () => {
  await markAllAsRead();
  await loadNotifications();
};

loadNotifications();
</script>

<style lang="scss" scoped>
.notification-center { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; h2 { margin: 0; } }
.notification-item {
  padding: 14px 16px;
  border-radius: 10px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: background 0.2s;
  background: #fff;
  border: 1px solid #f1f5f9;
  &.unread { background: #f0f9ff; border-color: #bae6fd; }
  &:hover { background: #f8fafc; }
  .item-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
  .item-title { font-weight: 600; font-size: 14px; display: flex; align-items: center; gap: 6px; }
  .unread-dot { width: 8px; height: 8px; border-radius: 50%; background: #0ea5e9; flex-shrink: 0; }
  .item-content { font-size: 13px; color: #64748b; margin: 0 0 6px 0; line-height: 1.5; }
  .item-time { font-size: 12px; color: #94a3b8; }
}
</style>
