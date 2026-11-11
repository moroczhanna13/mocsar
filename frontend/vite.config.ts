import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Fejlesztéskor a /api és a /ws a Spring Boot szerverre megy (8080).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws': { target: 'ws://localhost:8080', ws: true },
    },
  },
});
