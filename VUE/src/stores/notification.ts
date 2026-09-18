import { defineStore } from "pinia";
import { ref } from "vue";
import { getUnreadCount, type NotificationVO } from "@/api/notification";

export const useNotificationStore = defineStore("notification", () => {
  const unreadCount = ref(0);

  const fetchUnreadCount = async () => {
    try { unreadCount.value = await getUnreadCount(); }
    catch { /* ignore */ }
  };

  const incrementUnread = () => { unreadCount.value++; };

  const decrementUnread = () => { if (unreadCount.value > 0) unreadCount.value--; };

  return { unreadCount, fetchUnreadCount, incrementUnread, decrementUnread };
});
