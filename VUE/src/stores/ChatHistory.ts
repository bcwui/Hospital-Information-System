import { defineStore } from "pinia";

interface SessionIdObj {
  time: number;
  sessionId: string;
}

const STORAGE_KEY = "his_chat_sessions";

function loadFromStorage(): Map<string, SessionIdObj> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const entries: [string, SessionIdObj][] = JSON.parse(raw);
      // 过滤掉已过期的（超过1小时）
      const now = Date.now();
      const valid = entries.filter(([, v]) => now - v.time < 60 * 60 * 1000);
      return new Map(valid);
    }
  } catch { /* ignore */ }
  return new Map();
}

function saveToStorage(map: Map<string, SessionIdObj>) {
  try {
    const entries = Array.from(map.entries());
    localStorage.setItem(STORAGE_KEY, JSON.stringify(entries));
  } catch { /* ignore */ }
}

export const useChatHistoryStore = defineStore("chatHistory", () => {
  const sessionIdMap = loadFromStorage();

  const addId = (patientId: string, sessionId: string) => {
    sessionIdMap.set(patientId, { sessionId, time: Date.now() });
    saveToStorage(sessionIdMap);
  };

  const getId = (patientId: string) => {
    const obj = sessionIdMap.get(patientId);
    if (!obj) return "";
    if (Date.now() - obj.time > 60 * 60 * 1000) {
      sessionIdMap.delete(patientId);
      saveToStorage(sessionIdMap);
      return "";
    }
    // 续期
    obj.time = Date.now();
    saveToStorage(sessionIdMap);
    return obj.sessionId;
  };

  const clearSession = (patientId: string) => {
    sessionIdMap.delete(patientId);
    saveToStorage(sessionIdMap);
  };

  return { sessionIdMap, addId, getId, clearSession };
});
