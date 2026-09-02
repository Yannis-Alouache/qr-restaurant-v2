// Polyfill : sockjs-client (CommonJS, utilisé par le suivi temps réel des commandes)
// s'attend au `global` de Node.js, absent des navigateurs bundlés par Vite.
// Sans lui, le chunk de la page confirmation plante à l'import
// (« ReferenceError: global is not defined »).
if (typeof globalThis === 'object' && !('global' in globalThis)) {
  Object.defineProperty(globalThis, 'global', {
    value: globalThis,
    writable: true,
    configurable: true,
  });
}
