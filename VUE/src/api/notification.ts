import { DoAxiosWithErro } from "./index";

export interface NotificationVO {
  notificationId: number;
  title: string;
  content: string;
  type: string;
  isRead: number;
  relatedId: number;
  createTime: string;
}

export const getMyNotifications = async (): Promise<NotificationVO[]> => {
  try {
    return await DoAxiosWithErro<NotificationVO[]>("/notifications", "get", {}, true, false) ?? [];
  } catch (err) {
    console.error("获取通知失败:", err);
    throw err;
  }
};

export const getUnreadCount = async (): Promise<number> => {
  try {
    return await DoAxiosWithErro<number>("/notifications/unread-count", "get", {}, true, false) ?? 0;
  } catch (err) {
    console.error("获取未读数失败:", err);
    return 0;
  }
};

export const markAsRead = async (notificationId: number): Promise<boolean> => {
  try {
    return await DoAxiosWithErro<boolean>(`/notifications/${notificationId}/read`, "put", {}, true, true);
  } catch (err) {
    console.error("标记已读失败:", err);
    throw err;
  }
};

export const markAllAsRead = async (): Promise<boolean> => {
  try {
    return await DoAxiosWithErro<boolean>("/notifications/read-all", "put", {}, true, true);
  } catch (err) {
    console.error("全部已读失败:", err);
    throw err;
  }
};
