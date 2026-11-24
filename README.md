# Canteen Group Orders - LAN Chat

## Project Overview
A LAN-based group chat for canteen group orders. Multiple clients connect to one server over the same WiFi/LAN. Users can chat, join group orders, and see who is online in real time. Built in Java Swing with Socket programming.

## Members
- Prince Cristopher Rafon - GitHub: princecristopherrafon-star
- Dan Tuala - GitHub: Dan Tuala GitHub username

## Course and Section
ITAS1 - 2B - Iloilo Science and Technology University - Barotac Nuevo Campus

## Concept Proposal and Approval
Theme: Canteen Group Orders. Concept approved: LAN chat that helps students group their canteen orders to save time and avoid long queues.

## User Problem and Theme
Problem: Students go to the canteen one by one, long lines, repeated orders, and no coordination.
Solution: One group chat where classmates can combine orders, assign one person to buy, and track order status together.

## Core Features
- Multi-client group chat (3+ clients)
- Online users list with nickname updates
- Broadcast message to all clients
- Nickname change (valid and duplicate check)
- Blank message rejection
- Clean /exit and abrupt disconnect handling

## Original Features (2)
1. Group Order System - Commands: /order [item], /orders, /myorder, /cancelorder. Server keeps shared order list and broadcasts order updates to all clients.
2. Order Status / Ready Alert - Commands: /ready, /status. When order is ready, server broadcasts alert to all clients. Prevents duplicate orders.

## Architecture Diagram
Client (Swing GUI) <-> Socket (TCP) <-> Server (Multi-threaded) <-> Shared State (Online Users + Orders)
Message flow: Client -> Server -> All Clients

## Message Format
- Chat: CHAT|nickname|message
- Command: CMD|nickname|/order Burger
- User Update: USERS|user1,user2,user3
- System: SYSTEM|User joined / left
- Order Update: ORDER|nickname|Burger x1
- Error: ERROR|Message cannot be blank / Nickname already taken
Server validates all commands and shared state. Sends are synchronized to prevent concurrent sends from mixing.

## Setup Instructions
Requirements: Java 17+ (or Java 8+), NetBeans / IntelliJ / VS Code, Same WiFi / LAN.
1. Clone: git clone https://github.com/princecristopherrafon-star/ITAS1-2B-Rafon-Tuala.git
2. Open project in NetBeans.
3. Run ServerRafonTuala.java first.
4. Run ClientRafonTuala.java 3 times for 3 clients.
5. Enter IP: 127.0.0.1 for same PC, or Server PC LAN IP for other PC. Port: 5000 (check your code if different).

## Commands
- /order [item] - Add order
- /orders - Show all orders
- /myorder - Show my order
- /ready - Mark order as ready / broadcast ready alert
- /status - Show order status
- /nick [newname] - Change nickname
- /exit or /quit - Leave chat

## Test Results
| Test | Expected | Actual | Result |
| 3 concurrent clients | All connect | All connected | Pass |
| Broadcast to all | All receive | All received | Pass |
| Online list updates | List updates on join/leave | Updated | Pass |
| Valid nickname change | Changed | Changed | Pass |
| Duplicate nickname | Rejected | Rejected | Pass |
| Blank message | Rejected | Rejected | Pass |
| Invalid IP / Port | Error, no freeze | Error shown | Pass |
| Unavailable server | Error, no freeze | Error shown | Pass |
| Clean /exit | Removed from list | Removed | Pass |
| Abrupt disconnect | Removed, others continue | Removed | Pass |
| Original feature from 2 clients | Orders sync | Orders synced | Pass |

## Contribution Log
- 2026-11-20 - Tuala - Created Server, socket handling, broadcast
- 2026-11-21 - Rafon - Created Client GUI, nickname and online list
- 2026-11-22 - Rafon - Implemented Group Order feature
- 2026-11-23 - Tuala - Implemented Ready Alert / Status feature
- 2026-11-24 - Rafon & Tuala - Testing, README, final submission
