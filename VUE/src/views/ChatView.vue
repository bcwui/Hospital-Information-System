<template>
  <div class="chat-wrapper">
    <!-- 左侧历史对话列表 -->
    <div class="sidebar" v-if="couldSend">
      <div class="sidebar-header">
        <button @click="startNewChat" class="new-chat-button">+ 新建对话</button>
      </div>
      <div class="session-list">
        <div
          v-for="session in sessionList"
          :key="session.sessionId"
          :class="['session-item', { active: currentSessionId === session.sessionId }]"
          @click="switchSession(session.sessionId)"
        >
          <div class="session-preview">{{ session.preview || '新对话' }}</div>
          <div class="session-time">{{ formatTime(session.updateTime) }}</div>
        </div>
        <div v-if="sessionList.length === 0" class="no-sessions">暂无历史对话</div>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-container">
      <div class="chat-messages" ref="chatMessages">
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
              <div class="loading" v-if="message.sender === 'ai' && isLoading && index === messages.length - 1"></div>
            </div>
          </div>
        </div>
      </div>
      <div v-if="couldSend" class="chat-input">
        <textarea v-model="newMessage" @keypress.enter="sendMessage" placeholder="输入消息..." class="textarea-field"
          rows="3"></textarea>
        <button :disabled="isLoading" @click="sendMessage" class="send-button">发送</button>
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

// 配置marked选项
marked.setOptions({
  breaks: true, // 支持换行符转换为<br>
  gfm: true,    // 支持GitHub风格Markdown
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

// 解析Markdown为HTML
const parseMarkdown = (text: string): string => {
  if (!text) return '';
  return marked.parse(text) as string;
};

const pushHistory = (historyList: ChatHistoryEntry[]) => {
  historyList.forEach((item, index) => {
    if (index === 0) {
      return;
    }
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
    messages.push({
      sender: 'ai',
      text: value,
    });
  } else {
    last.text += value;
  }
  scrollToBottom();
};

// 加载会话列表
const loadSessionList = async () => {
  const patientId = getPatientId();
  if (!patientId) return;

  try {
    const res = await DoAxiosWithErro<SessionSummary[]>(
      '/appointment/ai-consult/sessions',
      'get',
      { patientId },
      true
    );
    sessionList.value = res || [];
  } catch (error) {
    console.log('获取会话列表失败:', error);
  }
};

// 切换会话
const switchSession = async (sessionId: string) => {
  if (sessionId === currentSessionId.value) return;

  abortController.value?.abort();
  isLoading.value = false;
  currentSessionId.value = sessionId;
  chatHistoryStore.addId(patientIdStr.value, sessionId);

  resetMessages();

  try {
    const res = await DoAxiosWithErro<{ messageHistory?: ChatHistoryEntry[] }>(
      '/appointment/ai-consult/history',
      'get',
      { sessionId },
      true
    );
    const history = res?.messageHistory || [];
    pushHistory(history);
  } catch (error) {
    console.log('获取会话历史失败:', error);
  }
};

// 新建对话
const startNewChat = () => {
  abortController.value?.abort();
  currentSessionId.value = '';
  // 从 Map 中删除缓存的 sessionId
  chatHistoryStore.sessionIdMap.delete(patientIdStr.value);
  resetMessages();
  isLoading.value = false;
};

/**
 * 发送消息 - 使用/stream接口（SSE流式）
 */
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

  // 显示用户消息
  messages.push({
    sender: 'user',
    text: trimmed,
  });
  newMessage.value = '';
  scrollToBottom();

  isLoading.value = true;

  // 中止之前的请求
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

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    // 从响应头获取sessionId
    const sessionId = response.headers.get('X-Session-Id');
    if (sessionId) {
      chatHistoryStore.addId(patientIdStr.value, sessionId);
      currentSessionId.value = sessionId;
      // 刷新会话列表
      loadSessionList();
    }

    // 读取SSE流
    const reader = response.body?.getReader();
    if (!reader) {
      throw new Error('无法读取响应流');
    }

    const decoder = new TextDecoder();
    let buffer = ''; // 缓冲区，用于处理跨chunk的不完整行

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      // 将新数据追加到缓冲区
      buffer += decoder.decode(value, { stream: true });

      // 按换行符分割，保留最后一个可能不完整的行
      const lines = buffer.split('\n');
      buffer = lines.pop() || ''; // 最后一个元素可能是不完整的行，保留到下次处理

      // SSE格式: data: xxx\n\n
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const text = line.slice(5).trim();
          if (text) {
            handleStreamMessage(text);
          }
        }
      }
    }

    // 处理缓冲区中剩余的数据
    if (buffer.startsWith('data:')) {
      const text = buffer.slice(5).trim();
      if (text) {
        handleStreamMessage(text);
      }
    }
  } catch (error: unknown) {
    if ((error as Error).name === 'AbortError') {
      return; // 用户主动取消
    }
    ElMessage.error('发送失败，请重试');
    console.error('Stream error:', error);
  } finally {
    isLoading.value = false;
  }
};

const getHistory = (sessionId: string) => {
  DoAxiosWithErro<{ messageHistory?: ChatHistoryEntry[] }>(
    '/appointment/ai-consult/history',
    'get',
    { sessionId },
    true
  ).then((res) => {
    const history: ChatHistoryEntry[] = Array.isArray(res?.messageHistory) ? res.messageHistory : [];
    pushHistory(history);
  });
};

onMounted(async () => {
  if (!props.couldSend) {
    return;
  }

  const patientId = getPatientId();
  if (!patientId) {
    return;
  }

  // 加载会话列表
  await loadSessionList();

  // 1. 先检查本地缓存的 sessionId
  const cachedSessionId = chatHistoryStore.getId(patientIdStr.value);
  if (cachedSessionId) {
    currentSessionId.value = cachedSessionId;
    getHistory(cachedSessionId);
    return;
  }

  // 2. 从数据库查询最近的会话
  try {
    const response = await DoAxiosWithErro<{ sessionId?: string; messageHistory?: ChatHistoryEntry[] }>(
      '/appointment/ai-consult/latest',
      'get',
      { patientId },
      true
    );
    if (response?.sessionId) {
      chatHistoryStore.addId(patientIdStr.value, response.sessionId);
      currentSessionId.value = response.sessionId;
      pushHistory(response.messageHistory || []);
    }
  } catch (error) {
    // 查询失败不影响正常使用，用户可以开始新对话
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
  background-color: #f5f7fa;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.sidebar {
  width: 240px;
  min-width: 240px;
  background-color: #e8f4fc;
  display: flex;
  flex-direction: column;
  color: #2c3e50;
  border-right: 1px solid #c5dff0;
}

.sidebar-header {
  padding: 15px;
  border-bottom: 1px solid #c5dff0;
}

.new-chat-button {
  width: 100%;
  background-color: #4a9cf7;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 10px 16px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
}

.new-chat-button:hover {
  background-color: #3a8ce7;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.session-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 8px;
  transition: background-color 0.2s;
  background-color: #f5fafd;
}

.session-item:hover {
  background-color: #d4ebf7;
}

.session-item.active {
  background-color: #b8ddf5;
  border-left: 3px solid #4a9cf7;
}

.session-preview {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
  color: #2c3e50;
}

.session-time {
  font-size: 11px;
  color: #7a9cb8;
}

.no-sessions {
  text-align: center;
  color: #7a9cb8;
  padding: 20px;
  font-size: 13px;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.message {
  display: flex;
  max-width: 100%;
  margin-bottom: 10px;
}

.message.user {
  justify-content: flex-end;
}

.message.user .message-avatar {
  order: 2;
}

.loading {
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  animation: spin 2s linear infinite;
  margin-top: 5px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  margin: 0 10px;
}

.message-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-content {
  background-color: white;
  padding: 10px 15px;
  border-radius: 18px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  max-width: 80%;
  word-wrap: break-word;
}

.message.user .message-content {
  background-color: #4a6cf7;
  color: white;
  border-radius: 18px 18px 0 18px;
}

.message-sender {
  font-size: 12px;
  font-weight: bold;
  margin-bottom: 4px;
}

.message-text {
  font-size: 14px;
}

/* Markdown内容样式 */
.markdown-content {
  line-height: 1.6;

  :deep(p) {
    margin: 0 0 8px 0;
    &:last-child {
      margin-bottom: 0;
    }
  }

  :deep(strong) {
    font-weight: 600;
  }

  :deep(em) {
    font-style: italic;
  }

  :deep(ul), :deep(ol) {
    margin: 8px 0;
    padding-left: 20px;
  }

  :deep(li) {
    margin: 4px 0;
  }

  :deep(h1), :deep(h2), :deep(h3), :deep(h4) {
    margin: 12px 0 8px 0;
    font-weight: 600;
  }

  :deep(h1) { font-size: 1.3em; }
  :deep(h2) { font-size: 1.2em; }
  :deep(h3) { font-size: 1.1em; }

  :deep(code) {
    background-color: rgba(0, 0, 0, 0.05);
    padding: 2px 6px;
    border-radius: 4px;
    font-family: monospace;
    font-size: 0.9em;
  }

  :deep(pre) {
    background-color: rgba(0, 0, 0, 0.05);
    padding: 12px;
    border-radius: 8px;
    overflow-x: auto;
    margin: 8px 0;

    code {
      background: none;
      padding: 0;
    }
  }

  :deep(blockquote) {
    border-left: 3px solid #4a6cf7;
    margin: 8px 0;
    padding-left: 12px;
    color: #666;
  }

  :deep(a) {
    color: #4a6cf7;
    text-decoration: none;
    &:hover {
      text-decoration: underline;
    }
  }

  :deep(table) {
    border-collapse: collapse;
    margin: 8px 0;
    width: 100%;
  }

  :deep(th), :deep(td) {
    border: 1px solid #ddd;
    padding: 8px;
    text-align: left;
  }

  :deep(th) {
    background-color: #f5f5f5;
  }
}

.chat-input {
  padding: 15px;
  background-color: white;
  display: flex;
  border-top: 1px solid #e0e0e0;
}

.textarea-field {
  flex: 1;
  padding: 12px 15px;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  outline: none;
  font-size: 14px;
  resize: none;
  overflow-y: auto;
  min-height: 60px;
  max-height: 120px;
}

.send-button {
  background-color: #4a6cf7;
  color: white;
  border: none;
  border-radius: 20px;
  padding: 12px 20px;
  margin-left: 10px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
}

.send-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .sidebar {
    width: 180px;
    min-width: 180px;
  }
}
</style>
