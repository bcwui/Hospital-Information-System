import { defineStore } from "pinia";
import { reactive } from "vue";

interface SessionIdObj {
  time: number;
  sessionId: string;
}

export const useChatHistoryStore = defineStore("chatHistory", () => {
  // 使用 patientId 作为 key 存储会话ID
  const sessionIdMap = reactive(new Map<string, SessionIdObj>());

  const addId = (patientId: string, sessionId: string) => {
    const newId: SessionIdObj = {
      sessionId,
      time: new Date().getTime(),
    };
    sessionIdMap.set(patientId, newId);
  };

  const getId = (patientId: string) => {
    const idObj = sessionIdMap.get(patientId);
    if (!idObj) {
      return "";
    }
    // 会话超过1小时过期
    if (new Date().getTime() - idObj.time > 60 * 60 * 1000) {
      sessionIdMap.delete(patientId);
      return "";
    }

    idObj.time = new Date().getTime();

    return idObj.sessionId;
  };

  // 清除会话，用于新建对话
  const clearSession = (patientId: string) => {
    sessionIdMap.delete(patientId);
  };

  return {
    sessionIdMap,
    addId,
    getId,
    clearSession,
  };
});
