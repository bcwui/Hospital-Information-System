<template>
  <div class="chat-wrapper">
    <!-- 左侧历史对话列表 -->
    <div class="sidebar" v-if="couldSend">
      <div class="sidebar-header">
        <span class="sidebar-title">历史对话</span>
        <button @click="startNewChat" class="new-chat-button">
          <svg viewBox="0 0 24 24" fill="none" width="16" height="16">
            <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          新建对话
        </button>
      </div>
      <div class="session-list">
        <div
          v-for="session in sessionList"
          :key="session.sessionId"
          :class="['session-item', { active: currentSessionId === session.sessionId }]"
          @click="switchSession(session.sessionId)"
        >
          <div class="session-content">
            <svg class="session-chat-icon" viewBox="0 0 24 24" fill="none" width="14" height="14">
              <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2v10z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <div class="session-preview">{{ session.preview || '新对话' }}</div>
          </div>
          <div class="session-time">{{ formatTime(session.updateTime) }}</div>
        </div>
        <div v-if="sessionList.length === 0" class="no-sessions">
          <svg viewBox="0 0 24 24" fill="none" width="36" height="36" style="opacity: 0.3; margin-bottom: 8px;">
            <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2v10z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          暂无历史对话
        </div>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-container">
      <div class="chat-header" v-if="couldSend">
        <span class="chat-header-title">
          {{ currentSessionId ? 'AI 健康咨询对话' : '新对话' }}
        </span>
      </div>
      <div class="chat-messages" ref="chatMessages">
        <div v-if="messages.length === 1 && messages[0].sender === 'ai'" class="welcome-screen">
          <div class="welcome-icon">
            <svg viewBox="0 0 80 80" fill="none">
              <circle cx="40" cy="40" r="38" stroke="url(#welGrad)" stroke-width="2" stroke-dasharray="6 4" fill="none"/>
              <path d="M28 40h24M40 28v24" stroke="url(#welGrad)" stroke-width="2.5" stroke-linecap="round"/>
              <defs>
                <linearGradient id="welGrad" x1="0" y1="0" x2="80" y2="80">
                  <stop stop-color="#0ea5e9"/>
                  <stop offset="1" stop-color="#6366f1"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <h3>AI 健康助手</h3>
          <p>我是您的智能健康咨询助手，有什么可以帮助您的吗？</p>
        </div>
        <div v-for="(message, index) in messages" :key="index" :class="['message', message.sender]">
          <div class="message-avatar">
            <img :src="avatars[message.sender === 'user' ? 0 : 1]" alt="头像" />
          </div>
          <div class="message-content">
            <div class="message-sender">{{ message.sender === 'user' ? '你' : 'AI助手' }}</div>
            <div class="message-text">
              <template v-if="message.sender === 'ai'">
                <div class="markdown-content" v-html="parseMarkdown(message.text)"></div>
              </template>
              <template v-else>{{ message.text }}</template>
              <div class="typing-indicator" v-if="message.sender === 'ai' && isLoading && index === messages.length - 1">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-if="couldSend" class="chat-input">
        <textarea
          v-model="newMessage"
          @keypress.enter="sendMessage"
          placeholder="输入您的问题，按 Enter 发送..."
          class="textarea-field"
          rows="2"
        ></textarea>
        <button :disabled="isLoading" @click="sendMessage" class="send-button">
          <svg v-if="!isLoading" viewBox="0 0 24 24" fill="none" width="20" height="20">
            <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span v-else class="sending-spinner"></span>
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted, onUnmounted, computed } from 'vue';
import { DoAxiosWithErro } from '@/api/index';
import { useUserStore } from '@/stores/user';
import { useChatHistoryStore } from '@/stores/ChatHistory';
import { ElMessage } from 'element-plus';
import { marked } from 'marked';
import meAvatar from '@/assets/me.png';
import aiAvatar from '@/assets/AIavator.png';

marked.setOptions({
  breaks: true,
  gfm: true,
});

const API_BASE = import.meta.env.VITE_API_BASE || '/api';
type ChatHistoryEntry = {
  role: string;
  content: string;
};

interface SessionSummary {
  sessionId: string;
  createTime: string;
  updateTime: string;
  preview: string;
}

const props = defineProps({
  couldSend: {
    type: Boolean,
    default: true,
  },
});

const avatars = [meAvatar, aiAvatar];

const messages = reactive([
  {
    sender: 'ai',
    text: '你好！有什么我可以帮助你的吗？',
  },
]);

const userStore = useUserStore();
const chatHistoryStore = useChatHistoryStore();

const isLoading = ref(false);
const newMessage = ref('');
const chatMessages = ref<HTMLElement | null>(null);
const abortController = ref<AbortController | null>(null);
const sessionList = ref<SessionSummary[]>([]);
const currentSessionId = ref<string>('');

const getPatientId = () => Number(userStore.userInfo?.patientId ?? 0);
const patientIdStr = computed(() => String(getPatientId()));

const scrollToBottom = () => {
  if (chatMessages.value) {
    chatMessages.value.scrollTop = chatMessages.value.scrollHeight;
  }
};

const formatTime = (timeStr: string) => {
  if (!timeStr) return '';
  const date = new Date(timeStr);
  const now = new Date();
  const isToday = date.toDateString() === now.toDateString();
  if (isToday) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  }
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' });
};

const parseMarkdown = (text: string): string => {
  if (!text) return '';
  return marked.parse(text) as string;
};

const pushHistory = (historyList: ChatHistoryEntry[]) => {
  historyList.forEach((item, index) => {
    if (index === 0) { return; }
    messages.push({
      sender: item.role === 'user' ? 'user' : 'ai',
      text: item.content,
    });
  });
  scrollToBottom();
};

const resetMessages = () => {
  messages.length = 0;
  messages.push({
    sender: 'ai',
    text: '你好！有什么我可以帮助你的吗？',
  });
};

const handleStreamMessage = (value: string) => {
  const last = messages[messages.length - 1];
  if (!last || last.sender === 'user') {
    messages.push({ sender: 'ai', text: value });
  } else {
    last.text += value;
  }
  scrollToBottom();
};

const loadSessionList = async () => {
  const patientId = getPatientId();
  if (!patientId) return;
  try {
    const res = await DoAxiosWithErro<SessionSummary[]>(
      '/appointment/ai-consult/sessions', 'get', { patientId }, true
    );
    sessionList.value = res || [];
  } catch (error) {
    console.log('获取会话列表失败:', error);
  }
};

const switchSession = async (sessionId: string) => {
  if (sessionId === currentSessionId.value) return;
  abortController.value?.abort();
  isLoading.value = false;
  currentSessionId.value = sessionId;
  chatHistoryStore.addId(patientIdStr.value, sessionId);
  resetMessages();
  try {
    const res = await DoAxiosWithErro<{ messageHistory?: ChatHistoryEntry[] }>(
      '/appointment/ai-consult/history', 'get', { sessionId }, true
    );
    const history = res?.messageHistory || [];
    pushHistory(history);
  } catch (error) {
    console.log('获取会话历史失败:', error);
  }
};

const startNewChat = () => {
  abortController.value?.abort();
  currentSessionId.value = '';
  chatHistoryStore.sessionIdMap.delete(patientIdStr.value);
  resetMessages();
  isLoading.value = false;
};

const sendMessage = async () => {
  const trimmed = newMessage.value.trim();
  if (!trimmed) {
    ElMessage.warning('消息不能为空');
    return;
  }
  const patientId = getPatientId();
  if (!patientId) {
    ElMessage.error('缺少患者信息');
    return;
  }
  messages.push({ sender: 'user', text: trimmed });
  newMessage.value = '';
  scrollToBottom();
  isLoading.value = true;

  abortController.value?.abort();
  const controller = new AbortController();
  abortController.value = controller;
  const tokenValue = userStore.userToken || localStorage.getItem('userToken') || '';

  try {
    const response = await fetch(`${API_BASE}/appointment/ai-consult/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'sa-token-authorization': tokenValue,
      },
      body: JSON.stringify({
        patientId,
        question: trimmed,
        sessionId: chatHistoryStore.getId(patientIdStr.value) || null,
      }),
      signal: controller.signal,
    });

    if (!response.ok) { throw new Error(`HTTP error! status: ${response.status}`); }

    const sessionId = response.headers.get('X-Session-Id');
    if (sessionId) {
      chatHistoryStore.addId(patientIdStr.value, sessionId);
      currentSessionId.value = sessionId;
      loadSessionList();
    }

    const reader = response.body?.getReader();
    if (!reader) { throw new Error('无法读取响应流'); }

    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const text = line.slice(5).trim();
          if (text) { handleStreamMessage(text); }
        }
      }
    }
    if (buffer.startsWith('data:')) {
      const text = buffer.slice(5).trim();
      if (text) { handleStreamMessage(text); }
    }
  } catch (error: unknown) {
    if ((error as Error).name === 'AbortError') { return; }
    ElMessage.error('发送失败，请重试');
    console.error('Stream error:', error);
  } finally {
    isLoading.value = false;
  }
};

const getHistory = (sessionId: string) => {
  DoAxiosWithErro<{ messageHistory?: ChatHistoryEntry[] }>(
    '/appointment/ai-consult/history', 'get', { sessionId }, true
  ).then((res) => {
    const history: ChatHistoryEntry[] = Array.isArray(res?.messageHistory) ? res.messageHistory : [];
    pushHistory(history);
  });
};

onMounted(async () => {
  if (!props.couldSend) { return; }
  const patientId = getPatientId();
  if (!patientId) { return; }
  await loadSessionList();
  const cachedSessionId = chatHistoryStore.getId(patientIdStr.value);
  if (cachedSessionId) {
    currentSessionId.value = cachedSessionId;
    getHistory(cachedSessionId);
    return;
  }
  try {
    const response = await DoAxiosWithErro<{ sessionId?: string; messageHistory?: ChatHistoryEntry[] }>(
      '/appointment/ai-consult/latest', 'get', { patientId }, true
    );
    if (response?.sessionId) {
      chatHistoryStore.addId(patientIdStr.value, response.sessionId);
      currentSessionId.value = response.sessionId;
      pushHistory(response.messageHistory || []);
    }
  } catch (error) {
    console.log('获取历史会话失败:', error);
  }
});

onUnmounted(() => {
  abortController.value?.abort();
});
</script>

<style lang="scss" scoped>
.chat-wrapper {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 0;
  background: #f8fafc;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.04);
}

/* Sidebar */
.sidebar {
  width: 260px;
  min-width: 260px;
  background: linear-gradient(180deg, #f0f9ff, #faf5ff);
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e2e8f0;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sidebar-title {
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: #94a3b8;
}

.new-chat-button {
  width: 100%;
  background: linear-gradient(135deg, #0ea5e9, #6366f1);
  color: white;
  border: none;
  border-radius: 10px;
  padding: 10px 14px;
  cursor: pointer;
  font-size: 0.88rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(14, 165, 233, 0.25);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 14px rgba(14, 165, 233, 0.35);
  }
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  padding: 12px 14px;
  border-radius: 10px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: all 0.2s ease;
  position: relative;

  &:hover {
    background: rgba(14, 165, 233, 0.06);
  }

  &.active {
    background: linear-gradient(135deg, rgba(14, 165, 233, 0.12), rgba(99, 102, 241, 0.08));
    border-left: 3px solid #0ea5e9;
  }
}

.session-content {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 4px;
}

.session-chat-icon {
  flex-shrink: 0;
  margin-top: 1px;
  color: #94a3b8;
}

.session-item.active .session-chat-icon {
  color: #0ea5e9;
}

.session-preview {
  font-size: 0.82rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #475569;
  line-height: 1.4;
}

.session-item.active .session-preview {
  color: #1e293b;
  font-weight: 500;
}

.session-time {
  font-size: 0.72rem;
  color: #94a3b8;
  padding-left: 22px;
}

.no-sessions {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #94a3b8;
  padding: 32px 12px;
  font-size: 0.85rem;
}

/* Chat Area */
.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  padding: 12px 20px;
  border-bottom: 1px solid #e2e8f0;
  background: rgba(255, 255, 255, 0.7);
}

.chat-header-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: #475569;
}

.chat-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Welcome Screen */
.welcome-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  gap: 12px;
  margin-top: 60px;

  .welcome-icon svg {
    filter: drop-shadow(0 4px 12px rgba(14, 165, 233, 0.15));
    animation: pulse-ring 3s ease-in-out infinite;
  }

  h3 {
    font-size: 1.3rem;
    font-weight: 700;
    color: #1e293b;
    margin: 0;
  }

  p {
    font-size: 0.9rem;
    color: #94a3b8;
    max-width: 280px;
    text-align: center;
    line-height: 1.6;
  }
}

@keyframes pulse-ring {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.05); opacity: 0.8; }
}

/* Messages */
.message {
  display: flex;
  max-width: 100%;
}

.message.user {
  justify-content: flex-end;
}

.message.user .message-avatar {
  order: 2;
}

.message-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  overflow: hidden;
  margin: 0 10px;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.message-content {
  padding: 12px 16px;
  border-radius: 18px;
  max-width: 75%;
  word-wrap: break-word;
  transition: box-shadow 0.2s ease;
}

.message.ai .message-content {
  background: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  border-bottom-left-radius: 6px;
}

.message.user .message-content {
  background: linear-gradient(135deg, #0ea5e9, #6366f1);
  color: white;
  border-bottom-right-radius: 6px;
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.3);
}

.message-sender {
  font-size: 0.72rem;
  font-weight: 600;
  margin-bottom: 4px;
  letter-spacing: 0.03em;
}

.message.user .message-sender {
  color: rgba(255, 255, 255, 0.8);
}

.message.ai .message-sender {
  color: #0ea5e9;
}

.message-text {
  font-size: 0.92rem;
  line-height: 1.6;
}

/* Typing Indicator */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 6px 0 2px;

  span {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #94a3b8;
    animation: typing 1.4s ease-in-out infinite;

    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

/* Input */
.chat-input {
  padding: 16px 20px;
  background: #ffffff;
  display: flex;
  align-items: flex-end;
  border-top: 1px solid #e2e8f0;
  gap: 10px;
}

.textarea-field {
  flex: 1;
  padding: 12px 16px;
  border: 1.5px solid #e2e8f0;
  border-radius: 14px;
  outline: none;
  font-size: 0.9rem;
  resize: none;
  overflow-y: auto;
  min-height: 48px;
  max-height: 100px;
  font-family: inherit;
  line-height: 1.5;
  transition: all 0.25s ease;
  background: #f8fafc;

  &:focus {
    border-color: #0ea5e9;
    box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.1);
    background: #fff;
  }

  &::placeholder {
    color: #94a3b8;
  }
}

.send-button {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #0ea5e9, #6366f1);
  color: white;
  border: none;
  border-radius: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.25);
  flex-shrink: 0;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 6px 18px rgba(14, 165, 233, 0.35);
  }

  &:active:not(:disabled) {
    transform: translateY(0);
  }

  &:disabled {
    background: #cbd5e1;
    box-shadow: none;
    cursor: not-allowed;
  }
}

.sending-spinner {
  width: 20px;
  height: 20px;
  border: 2.5px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Markdown */
.markdown-content {
  line-height: 1.7;

  :deep(p) { margin: 0 0 6px 0; &:last-child { margin-bottom: 0; } }
  :deep(strong) { font-weight: 600; }
  :deep(em) { font-style: italic; }
  :deep(ul), :deep(ol) { margin: 6px 0; padding-left: 20px; }
  :deep(li) { margin: 3px 0; }
  :deep(h1), :deep(h2), :deep(h3), :deep(h4) { margin: 10px 0 6px 0; font-weight: 600; }
  :deep(h1) { font-size: 1.25em; }
  :deep(h2) { font-size: 1.15em; }
  :deep(h3) { font-size: 1.05em; }
  :deep(code) { background: rgba(0, 0, 0, 0.05); padding: 2px 6px; border-radius: 4px; font-family: monospace; font-size: 0.88em; }
  :deep(pre) { background: #f1f5f9; padding: 12px; border-radius: 10px; overflow-x: auto; margin: 6px 0; code { background: none; padding: 0; } }
  :deep(blockquote) { border-left: 3px solid #0ea5e9; margin: 8px 0; padding-left: 12px; color: #64748b; }
  :deep(a) { color: #0ea5e9; text-decoration: none; &:hover { text-decoration: underline; } }
  :deep(table) { border-collapse: collapse; margin: 6px 0; width: 100%; }
  :deep(th), :deep(td) { border: 1px solid #e2e8f0; padding: 8px; text-align: left; }
  :deep(th) { background: #f8fafc; font-weight: 600; }
}

.markdown-content:deep(*) {
  color: inherit;
}

.message.user .markdown-content:deep(*) {
  color: rgba(255, 255, 255, 0.95);
}

@media (max-width: 768px) {
  .sidebar {
    width: 200px;
    min-width: 200px;
  }

  .message-content {
    max-width: 85%;
  }
}
</style>
