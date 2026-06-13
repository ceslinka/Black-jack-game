import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getToken } from './api'

export function createStompClient({ onConnect, onDisconnect, onError }) {
  let reportedError = false

  const client = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: {
      Authorization: `Bearer ${getToken()}`,
    },
    connectionTimeout: 10000,
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      reportedError = false
      onConnect?.(client)
    },
    onDisconnect: () => onDisconnect?.(),
    onStompError: (frame) => {
      console.warn('STOMP error', frame.headers?.message || frame.body)
      if (!reportedError) {
        reportedError = true
        onError?.(frame.headers?.message || 'Błąd połączenia WebSocket')
      }
    },
    onWebSocketClose: () => onDisconnect?.(),
  })

  client.activate()
  return client
}

export function subscribeTable(client, tableId, handler) {
  if (!client?.connected) return null
  return client.subscribe(`/topic/table/${tableId}`, (msg) => {
    handler(JSON.parse(msg.body))
  })
}

export function sendHit(client, tableId, handId) {
  if (!client?.connected) throw new Error('WebSocket nie jest połączony')
  client.publish({
    destination: '/app/action.hit',
    body: JSON.stringify({ tableId, handId }),
  })
}

export function sendStand(client, tableId, handId) {
  if (!client?.connected) throw new Error('WebSocket nie jest połączony')
  client.publish({
    destination: '/app/action.stand',
    body: JSON.stringify({ tableId, handId }),
  })
}

export function subscribeErrors(client, handler) {
  if (!client?.connected) return null
  return client.subscribe('/user/queue/errors', (msg) => {
    handler(JSON.parse(msg.body))
  })
}
