import type { Socket } from "socket.io-client";
import { socketClient } from "./socketClient";

interface SocketCallbacks {
  onOnlineUsers: (user_id: number[]) => void;
}

class SocketService {
  private socket: Socket | null = null;
  
  listenEvents(callbacks: SocketCallbacks): void {
  this.socket = socketClient.getSocket();
    if (this.socket) {
      this.socket.off("onlineUsers"); // off current listeners before adding a new one
      this.socket.on("onlineUsers", (user_id: number[]) => {
        callbacks.onOnlineUsers(user_id); // listen the new one and update to callback interface
      });
    }
  }
  
  removeListeners(): void {
    if (this.socket) {
      this.socket.off("onlineUsers");
      this.socket = null;
    }
  }
}

export const socketService = new SocketService();