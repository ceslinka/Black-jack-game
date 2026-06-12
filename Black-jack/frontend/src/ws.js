import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getToken } from './api'

export function createStompClient(onConnect) {
  const client = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: {
      Authorization: `Bearer ${getToken()}`,
    },
    onConnect: () => onConnect(client),
    onStompError: (frame) => console.error('STOMP error', frame),
    reconnectDelay: 3000,
  })
  client.activate()
  return client
}

export function subscribeTable(client, tableId, handler) {
  return client.subscribe(`/topic/table/${tableId}`, (msg) => {
    handler(JSON.parse(msg.body))
  })
}

export function sendJoin(client, tableId, seatIndex, asDealer) {
  client.publish({
    destination: '/app/table.join',
    body: JSON.stringify({ tableId, seatIndex, asDealer }),
  })
}

export function sendHit(client, tableId, handId) {
  client.publish({
    destination: '/app/action.hit',
    body: JSON.stringify({ tableId, handId }),
  })
}

export function sendStand(client, tableId, handId) {
  client.publish({
    destination: '/app/action.stand',
    body: JSON.stringify({ tableId, handId }),
  })
}

export function subscribeErrors(client, handler) {
  return client.subscribe('/user/queue/errors', (msg) => {
    handler(JSON.parse(msg.body))
  })
}
