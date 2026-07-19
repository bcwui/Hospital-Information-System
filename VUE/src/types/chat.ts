export interface AiEvent {
  type: 'text' | 'tool_call' | 'tool_result' | 'card' | 'ref' | 'done' | 'error'
  content?: string
  toolName?: string
  toolLabel?: string
  status?: string
  cardType?: string
  data?: any
  sources?: RefSource[]
  sessionId?: string
  message?: string
}

export interface RefSource {
  title: string
  snippet: string
}

export type MessageBlock =
  | { type: 'text'; content: string }
  | { type: 'toolStep'; toolName: string; label: string; status: 'running' | 'success' | 'error' }
  | { type: 'card'; cardType: string; data: any }
  | { type: 'ref'; sources: RefSource[] }
  | { type: 'error'; message: string }

export interface ChatMessage {
  sender: 'user' | 'ai'
  blocks: MessageBlock[]
}
