import { iniciarPagina } from '../core/layout.js';

await iniciarPagina({ trilha: [{ texto: 'Início', href: '/' }, { texto: document.querySelector('h1').textContent }] });
