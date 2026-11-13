import { io, type Socket } from "socket.io-client";

export const createSocket = (): Socket => {
  return io("/", {
    path: "/socket.io",
    withCredentials: true,
    transports: ["websocket", "polling"],
  });
};