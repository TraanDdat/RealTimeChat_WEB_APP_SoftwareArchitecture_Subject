import { io, type Socket } from "socket.io-client";
import { API_BASE_URL } from "../../config";

class SocketClient {
  private socket: Socket | null = null;

  create(): Socket {
    if (!this.socket) {
      this.socket = io(API_BASE_URL, {
        path: "/socket.io",
        withCredentials: true,
        transports: ["websocket", "polling"],
        autoConnect: false,
      });
    }
    return this.socket;
  }

  getSocket(): Socket | null {
    return this.socket;
  }

  disconnect(): void {
    if (this.socket) {
      this.socket.removeAllListeners();
      this.socket.disconnect();
      this.socket = null;
    }
  }
}

export const socketClient = new SocketClient();
export const socket = socketClient.create();