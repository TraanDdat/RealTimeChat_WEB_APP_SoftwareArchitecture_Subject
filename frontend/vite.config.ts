import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
    server: {
        proxy: {
            "/api": {
                target: "http://15.135.82.200:8000",
                changeOrigin: true
            },
            "/socket.io": {
                target: "http://15.135.82.200:8000",
                changeOrigin: true,
                ws: true
            }
        }
    }
})
