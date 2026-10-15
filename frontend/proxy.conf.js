// Proxy de desarrollo (ng serve): /api -> backend local. La API key se lee del entorno
// (API_KEY), nunca se escribe en el código ni llega al bundle del navegador.
module.exports = {
  '/api': {
    target: process.env.API_URL || 'http://localhost:8080',
    changeOrigin: true,
    secure: false,
    pathRewrite: { '^/api': '' },
    headers: { 'x-api-key': process.env.API_KEY || '' },
  },
};
